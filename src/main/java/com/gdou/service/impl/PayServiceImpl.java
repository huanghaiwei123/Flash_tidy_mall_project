package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gdou.Constant.RedisConstant;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.common.Result;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.SeckillOrderMapper;
import com.gdou.mapper.UserSeckillRecordMapper;
import com.gdou.pojo.entity.SeckillOrder;
import com.gdou.pojo.entity.UserSeckillRecord;
import com.gdou.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class PayServiceImpl implements PayService {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private UserSeckillRecordMapper userSeckillRecordMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * mock支付：直接改状态为已支付
     */
    @Override
    @Transactional
    public Result pay(Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在", ResultCodeConstant.ERROR);
        }
        if (!order.getState().equals(ResultCodeConstant.UNPAY)) {
            throw new BusinessException("订单状态异常，无法支付", ResultCodeConstant.ERROR);
        }

        // 更新订单状态 → 已支付
        LambdaUpdateWrapper<SeckillOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SeckillOrder::getOrderId, orderId);
        wrapper.set(SeckillOrder::getState, ResultCodeConstant.PAY_SUCCESS);
        wrapper.set(SeckillOrder::getPayTime, LocalDateTime.now());
        seckillOrderMapper.update(null, wrapper);

        // 更新秒杀记录状态 → 已支付
        LambdaUpdateWrapper<UserSeckillRecord> recordWrapper = new LambdaUpdateWrapper<>();
        recordWrapper.eq(UserSeckillRecord::getOrderId, orderId);
        recordWrapper.set(UserSeckillRecord::getState, ResultCodeConstant.PAY_SUCCESS);
        userSeckillRecordMapper.update(null, recordWrapper);

        log.info("订单 {} 支付成功", orderId);
        return Result.success("支付成功");
    }

    /**
     * 取消支付：释放库存回 Redis
     */
    @Override
    @Transactional
    public Result cancel(Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在", ResultCodeConstant.ERROR);
        }
        if (!order.getState().equals(ResultCodeConstant.UNPAY)) {
            throw new BusinessException("订单状态异常，无法取消", ResultCodeConstant.ERROR);
        }

        // 更新订单状态 → 已取消
        LambdaUpdateWrapper<SeckillOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SeckillOrder::getOrderId, orderId);
        wrapper.set(SeckillOrder::getState, ResultCodeConstant.PAY_CANCEL);
        seckillOrderMapper.update(null, wrapper);

        // 更新秒杀记录状态 → 已取消
        LambdaUpdateWrapper<UserSeckillRecord> recordWrapper = new LambdaUpdateWrapper<>();
        recordWrapper.eq(UserSeckillRecord::getOrderId, orderId);
        recordWrapper.set(UserSeckillRecord::getState, ResultCodeConstant.PAY_CANCEL);
        userSeckillRecordMapper.update(null, recordWrapper);

        // 回补 Redis 库存 + 删除用户防重标记
        String stockKey = RedisConstant.SECKILL_STOCK + order.getSeckillId();
        String userKey = RedisConstant.USER_SECKILL_RECORD + ":" + order.getUserId() + ":" + order.getSeckillId();
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(userKey);

        log.info("订单 {} 已取消，库存已回补", orderId);
        return Result.success("取消成功");
    }
}
