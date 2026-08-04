package com.gdou.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.Constant.RedisConstant;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.captcha.pojo.CaptchaVO;
import com.gdou.captcha.service.CaptchaService;
import com.gdou.common.Result;
import com.gdou.config.SeckillBloomFilter;
import com.gdou.exception.BusinessException;
import com.gdou.mq.SeckillMessage;
import com.gdou.mq.SeckillMqSender;
import com.gdou.pojo.dto.SeckillDto;
import com.gdou.mapper.SeckillOrderMapper;
import com.gdou.mapper.UserSeckillRecordMapper;
import com.gdou.pojo.entity.Seckill;
import com.gdou.service.SeckillService;
import com.gdou.mapper.SeckillMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* @author huanghaiwei
* @description 针对表【seckill(秒杀库存表)】的数据库操作Service实现
* @createDate 2026-07-26 16:48:10
*/
@Service
@Slf4j
public class SeckillServiceImpl extends ServiceImpl<SeckillMapper, Seckill>
    implements SeckillService{
    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private UserSeckillRecordMapper userSeckillRecordMapper;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Resource(name="SeckillScript")
    private DefaultRedisScript script;
    @Autowired
    private SeckillMqSender seckillMqSender;
    @Autowired
    private SeckillBloomFilter seckillBloomFilter;
    @Autowired
    private CaptchaService captchaService;

    @Override
    public List<Seckill> getSeckillList() {
        LambdaQueryWrapper<Seckill> wrapper = new LambdaQueryWrapper<>();
        return  seckillMapper.selectList(wrapper);
    }

    @Override
    public Result onSeckill(Long seckillId, String userId, String captchaAnswer) {
        // 布隆过滤器快速拦截不存在的 ID
        if (!seckillBloomFilter.mightContain(seckillId)) {
            throw new BusinessException("秒杀商品不存在", ResultCodeConstant.ERROR);
        }

        // 校验验证码
        String captchaKey = RedisConstant.CAPTCHA_KEY + ":{" + seckillId + "}:" + userId;
        Object answerObj = redisTemplate.opsForValue().get(captchaKey);
        String realAnswer = answerObj != null ? answerObj.toString() : null;
        if (realAnswer == null || !realAnswer.equals(captchaAnswer)) {
            throw new BusinessException("验证码不正确", ResultCodeConstant.ERROR);
        }
        redisTemplate.delete(captchaKey); // 校验通过立即删除，防止重用

        // hash tag {seckillId} 保证同一次秒杀的所有 key 落在 Redis Cluster 同一 slot
        String stockKey = RedisConstant.SECKILL_STOCK + ":{" + seckillId + "}";
        String userSeckillRecordKey = RedisConstant.USER_SECKILL_RECORD + ":{" + seckillId + "}:" + userId;

        // 构建 13 个 KEYS：[用户记录, bucket0~9, startTime, endTime]
        String startKey = RedisConstant.SECKILL_START + ":{" + seckillId + "}";
        String endKey = RedisConstant.SECKILL_END + ":{" + seckillId + "}";
        List<String> keys = new ArrayList<>();
        keys.add(userSeckillRecordKey);                    // KEYS[1]
        for (int i = 0; i < RedisConstant.BUCKET_COUNT; i++) {
            keys.add(stockKey + ":" + i);                  // KEYS[2] ~ KEYS[11]
        }
        keys.add(startKey);                                // KEYS[12]
        keys.add(endKey);                                  // KEYS[13]

        // Lua 脚本：时间校验 + 防重 + 随机选桶扣库存（一次网络往返）
        Long result = (Long) redisTemplate.execute(script, keys, System.currentTimeMillis());
        if (result == -4) {
            throw new BusinessException("该秒杀商品不存在，请重试", ResultCodeConstant.ERROR);
        } else if (result == -3) {
            throw new BusinessException("秒杀活动已结束", ResultCodeConstant.SeckillOver);
        } else if (result == -2) {
            throw new BusinessException("秒杀活动未开启", ResultCodeConstant.SeckillClose);
        } else if (result == -1) {
            throw new BusinessException("用户已经参与过此次秒杀活动", ResultCodeConstant.ERROR);
        } else if (result == null || result == 0) {
            throw new BusinessException("库存不足", ResultCodeConstant.StockEmpty);
        }

        // result 为 1~10，转成 0-based bucketId
        int bucketId = result.intValue() - 1;
        log.info("用户 {} 秒杀 {}，命中桶 {}，Redis Lua 扣库存成功", userId, seckillId, bucketId);

        // MQ 异步创建订单，携带 bucketId 用于失败回滚
        SeckillMessage message = SeckillMessage.builder()
                .seckillId(seckillId)
                .userId(userId)
                .bucketId(bucketId)
                .build();
        seckillMqSender.send(message);
        return Result.success("下单成功，正在自动为您创建订单");
    }

    @Override
    public Result addSeckill(SeckillDto seckillDto) {
        Seckill seckill = new Seckill();
        BeanUtils.copyProperties(seckillDto, seckill);
        seckill.setCreateTime(LocalDateTime.now());
        seckill.setVersion(0);
        seckillMapper.insert(seckill);
        log.info("新增秒杀活动成功，seckillId={}, name={}", seckill.getSeckillId(), seckill.getName());
        return Result.success(seckill);
    }

    @Override
    public Result updateSeckill(Long id, SeckillDto seckillDto) {
        Seckill seckill = seckillMapper.selectById(id);
        if (seckill == null) {
            throw new BusinessException("秒杀活动不存在", ResultCodeConstant.ERROR);
        }
        BeanUtils.copyProperties(seckillDto, seckill);
        seckill.setSeckillId(id);
        seckillMapper.updateById(seckill);
        log.info("修改秒杀活动成功，seckillId={}", id);
        return Result.success(seckill);
    }

    @Override
    public Result deleteSeckill(Long id) {
        Seckill seckill = seckillMapper.selectById(id);
        if (seckill == null) {
            throw new BusinessException("秒杀活动不存在", ResultCodeConstant.ERROR);
        }
        seckillMapper.deleteById(id);
        log.info("删除秒杀活动成功，seckillId={}", id);
        return Result.success();
    }

    @Override
    public Result getToken(Long seckillId, String userId) {
        if (!seckillBloomFilter.mightContain(seckillId)) {
            throw new BusinessException("商品id不存在", ResultCodeConstant.ERROR);
        }
        String key = RedisConstant.CAPTCHA_KEY + ":{" + seckillId + "}:" + userId;
        CaptchaVO captchaVO = captchaService.generate();
        redisTemplate.opsForValue().set(key, captchaVO.getAnswer(), 60, TimeUnit.SECONDS);
        Map<String, String> map = new HashMap<>();
        map.put("captcha_expression", captchaVO.getExpression());
        map.put("captcha_image", captchaVO.getImage());
        return Result.success(map);
    }
}
