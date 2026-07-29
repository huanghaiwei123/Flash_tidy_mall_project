package com.gdou.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.Constant.RedisConstant;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.common.Result;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.SeckillOrderMapper;
import com.gdou.mapper.UserSeckillRecordMapper;
import com.gdou.pojo.entity.Seckill;
import com.gdou.pojo.entity.SeckillOrder;
import com.gdou.pojo.entity.UserSeckillRecord;
import com.gdou.service.SeckillService;
import com.gdou.mapper.SeckillMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(rollbackFor = Exception.class)
    public Result onSeckill(Long seckillId, String userId) {
        String stockKey=RedisConstant.SECKILL_STOCK+seckillId;
        String userSeckillRecordKey = RedisConstant.USER_SECKILL_RECORD+":"+userId+":"+seckillId;
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
//        数据库扣库存和创建订单
        try {
            int count = seckillMapper.deductById(seckillId);
            if (count == 0) {
                throw new BusinessException("redis预热库存与DB库存不一致", ResultCodeConstant.ERROR);
            }
//            获取商品名称
            String name = seckill.getName();
//            秒杀订单创建
            SeckillOrder seckillOrder = SeckillOrder.builder()
                    .seckillId(seckillId)
                    .userId(Long.valueOf(userId))
                    .goodsName(name)
                    .price(seckill.getPrice())
                    .state(ResultCodeConstant.UNPAY)
                    .build();
//            插入秒杀订单信息
            seckillOrderMapper.insert(seckillOrder);
//            用户秒杀记录表
            UserSeckillRecord userSeckillRecord = UserSeckillRecord.builder()
                    .seckillId(seckillId)
                    .userId(Long.valueOf(userId))
                    .orderId(seckillOrder.getOrderId())
                    .state(ResultCodeConstant.UNPAY)
                    .build();
//            插入用户秒杀用户表
            userSeckillRecordMapper.insert(userSeckillRecord);
            log.info("用户 {} 秒杀成功，订单ID {}", userId, seckillOrder.getOrderId());
        }catch (BusinessException e){
//            删除秒杀记录,复原库存
            redisTemplate.delete(userSeckillRecordKey);
            redisTemplate.opsForValue().increment(stockKey);
            throw e;
        }
        return Result.success("秒杀成功");
    }

    /**
     * 管理员预热库存
     * @param seckillId
     * @return
     */
    @Override
    public Result warmStock(Long seckillId) {
        Seckill seckill = seckillMapper.selectById(seckillId);
        if(seckill==null){
            return Result.error("该商品不存在",ResultCodeConstant.ERROR,null);
        }
        redisTemplate.opsForValue().set(RedisConstant.SECKILL_STOCK+seckillId,seckill.getNumber());
        log.info("{}号商品redis预热库存成功", seckillId);
        return Result.success();
    }
}




