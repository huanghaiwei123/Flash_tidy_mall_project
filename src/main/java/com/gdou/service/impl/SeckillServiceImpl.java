package com.gdou.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.Constant.RedisConstant;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.common.Result;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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
    @Autowired
    private DefaultRedisScript script;
    @Autowired
    private SeckillMqSender seckillMqSender;
    /**
     * 获取秒杀商品列表
     * @return
     */
    @Override
    public List<Seckill> getSeckillList() {
        LambdaQueryWrapper<Seckill> wrapper = new LambdaQueryWrapper<>();
        return  seckillMapper.selectList(wrapper);
    }

    /**
     * 秒杀业务具体实现
     * @return
     */
    @Override
    public Result onSeckill(Long seckillId, String userId) {
        String stockKey=RedisConstant.SECKILL_STOCK+seckillId;
        String userSeckillRecordKey = RedisConstant.USER_SECKILL_RECORD+":"+userId+":"+seckillId;
        if(Boolean.FALSE.equals(redisTemplate.hasKey(stockKey))){
            throw new BusinessException("该秒杀商品不存在，请重试",ResultCodeConstant.ERROR);
        }
//        只让一个用户去查商品是否存在，防止大量用户压垮数据库
//        查商品是否存在，不存在抛异常
        Seckill seckill = seckillMapper.selectById(seckillId);
        if(seckill==null){
            throw new BusinessException("商品不存在", ResultCodeConstant.ERROR);
        }
//        判断秒杀活动是否开启或结束
        if(seckill.getStartTime().isAfter(LocalDateTime.now())){
            throw new BusinessException("秒杀活动未开启", ResultCodeConstant.SeckillClose);
        } else if (seckill.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("秒杀活动已结束", ResultCodeConstant.SeckillOver);
        }
//       lua脚本防止重复下单和原子扣库存
        Long execute = (Long) redisTemplate.execute(script, Arrays.asList(userSeckillRecordKey,stockKey));
        if(execute==-1){
            throw new BusinessException("用户已经参与过此次秒杀活动", ResultCodeConstant.ERROR);
        }else if(execute==null||execute==0){
            throw new BusinessException("库存不足", ResultCodeConstant.StockEmpty);
        }
        log.info("用户 {} 秒杀 {}，Redis Lua 扣库存成功", userId, seckillId);
//        MQ异步创建订单
        SeckillMessage message = SeckillMessage.builder().seckillId(seckillId).userId(userId).build();
        seckillMqSender.send(message);
        return Result.success("下单成功，已正在自动为您创建订单");
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
}




