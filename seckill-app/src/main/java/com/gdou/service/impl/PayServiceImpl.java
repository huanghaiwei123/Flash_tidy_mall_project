package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gdou.Constant.RedisConstant;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.common.Result;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.SeckillOrderMapper;
import com.gdou.mapper.SeckillMapper;
import com.gdou.mapper.UserSeckillRecordMapper;
import com.gdou.pojo.entity.Seckill;
import com.gdou.pojo.entity.SeckillOrder;
import com.gdou.pojo.entity.UserSeckillRecord;
import com.gdou.service.AlipayService;
import com.gdou.service.PayService;
import com.gdou.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class PayServiceImpl implements PayService {
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private UserSeckillRecordMapper userSeckillRecordMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private AlipayService alipayService;
    @Autowired
    private SeckillService seckillService;
    @Autowired
    private SeckillMapper seckillMapper;

    /**
     * 支付宝电脑网站支付：生成支付页面HTML（不修改订单状态，等支付宝异步回调）
     */
    @Override
    public String pay(Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在", ResultCodeConstant.ERROR);
        }
        if (!order.getState().equals(ResultCodeConstant.UNPAY)) {
            throw new BusinessException("订单状态异常，无法支付", ResultCodeConstant.ERROR);
        }
        String generatePayPage = alipayService.generatePayPage(order);
        log.info("订单 {} 已生成支付页面", orderId);
        return generatePayPage;
    }

    /**
     * 取消支付：释放库存回 Redis
     */
    @Override
    @Transactional
    public Result cancel(Long orderId) {
        // CAS 原子更新：只有 UNPAY 才改为 PAY_CANCEL
        LambdaUpdateWrapper<SeckillOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SeckillOrder::getOrderId, orderId)
               .eq(SeckillOrder::getState, ResultCodeConstant.UNPAY);  // CAS 条件
        wrapper.set(SeckillOrder::getState, ResultCodeConstant.PAY_CANCEL);
        int rows = seckillOrderMapper.update(null, wrapper);

        if (rows == 0) {
            SeckillOrder order = seckillOrderMapper.selectById(orderId);
            if (order == null) {
                throw new BusinessException("订单不存在", ResultCodeConstant.ERROR);
            }
            if (order.getState().equals(ResultCodeConstant.PAY_SUCCESS)) {
                throw new BusinessException("订单已支付，无法取消", ResultCodeConstant.ERROR);
            }
            throw new BusinessException("订单状态异常，无法取消", ResultCodeConstant.ERROR);
        }

        // CAS 成功 → 取订单信息（需要 bucketId 精确回滚）
        SeckillOrder order = seckillOrderMapper.selectById(orderId);

        // 更新秒杀记录状态 → 已取消
        LambdaUpdateWrapper<UserSeckillRecord> recordWrapper = new LambdaUpdateWrapper<>();
        recordWrapper.eq(UserSeckillRecord::getOrderId, orderId);
        recordWrapper.set(UserSeckillRecord::getState, ResultCodeConstant.PAY_CANCEL);
        userSeckillRecordMapper.update(null, recordWrapper);

        // 回补 DB 库存
        seckillMapper.incrementById(order.getSeckillId());

        // 回补 Redis 库存 — 用订单记录的 bucketId 精确回滚
        Integer bucketId = order.getBucketId();
        String stockKey = RedisConstant.SECKILL_STOCK + ":{" + order.getSeckillId() + "}:" + bucketId;
        String userKey = RedisConstant.USER_SECKILL_RECORD + ":{" + order.getSeckillId() + "}:" + order.getUserId();
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(userKey);

        log.info("订单 {} 已取消，DB库存+1，Redis桶{}库存+1", orderId, bucketId);
        return Result.success("取消成功");
    }

    /**
     * 订单退款
     */
    @Override
    public Result refund(Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在", ResultCodeConstant.ERROR);
        }
        if (!order.getState().equals(ResultCodeConstant.PAY_SUCCESS)) {
            throw new BusinessException("订单状态异常，仅已支付订单可退款", ResultCodeConstant.ERROR);
        }

        // 调支付宝退款接口
        alipayService.executeRefund(order);

        // 更新订单状态 → 已退款
        LambdaUpdateWrapper<SeckillOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SeckillOrder::getOrderId, orderId);
        wrapper.set(SeckillOrder::getState, ResultCodeConstant.REFUND);
        seckillOrderMapper.update(null, wrapper);

        // 更新秒杀记录状态 → 已退款
        LambdaUpdateWrapper<UserSeckillRecord> recordWrapper = new LambdaUpdateWrapper<>();
        recordWrapper.eq(UserSeckillRecord::getOrderId, orderId);
        recordWrapper.set(UserSeckillRecord::getState, ResultCodeConstant.REFUND);
        userSeckillRecordMapper.update(null, recordWrapper);

        // 回滚库存
        LambdaUpdateWrapper<Seckill> wrapper1 = new LambdaUpdateWrapper<>();
        wrapper1.eq(Seckill::getSeckillId, order.getSeckillId());
        wrapper1.setSql("number=number+1");
        log.info("订单 {} 已退款", orderId);
        return Result.success("退款成功");

    }

    @Override
    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        // 验签（密钥配置错误或伪造请求，重试无用，返回 success 阻止重试）
        if (!alipayService.verifyNotify(params)) {
            log.error("支付宝回调验签失败，疑似密钥配置错误或伪造请求");
            return "success";
        }

        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            log.info("交易未成功，状态: {}", tradeStatus);
            return "success";
        }

        Long orderId;
        try {
            orderId = Long.valueOf(params.get("out_trade_no"));
        } catch (NumberFormatException e) {
            log.error("回调订单号格式异常: {}", params.get("out_trade_no"));
            return "success";
        }
        String tradeNo = params.get("trade_no");

        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            log.error("回调订单不存在: {}", orderId);
            return "failure";
        }

        // 幂等：已支付直接返回成功
        if (order.getState().equals(ResultCodeConstant.PAY_SUCCESS)) {
            log.info("订单 {} 已支付，回调幂等返回", orderId);
            return "success";
        }

        if (!order.getState().equals(ResultCodeConstant.UNPAY)) {
            log.warn("订单 {} 状态异常: {}", orderId, order.getState());
            return "failure";
        }

        // CAS 原子更新：只有 UNPAY 才改为 PAY_SUCCESS
        LambdaUpdateWrapper<SeckillOrder> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SeckillOrder::getOrderId, orderId)
               .eq(SeckillOrder::getState, ResultCodeConstant.UNPAY);  // CAS 条件
        wrapper.set(SeckillOrder::getState, ResultCodeConstant.PAY_SUCCESS);
        wrapper.set(SeckillOrder::getTradeNo, tradeNo);
        wrapper.set(SeckillOrder::getPayTime, LocalDateTime.now());
        int row = seckillOrderMapper.update(null, wrapper);
        if (row == 0) {
            // CAS 失败：订单已被超时取消抢占了，返回 success 停止支付宝重试
            log.info("订单{}状态已变更，跳过支付回调", orderId);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "success";
        }
        // 更新秒杀记录
        LambdaUpdateWrapper<UserSeckillRecord> recordWrapper = new LambdaUpdateWrapper<>();
        recordWrapper.eq(UserSeckillRecord::getOrderId, orderId);
        recordWrapper.set(UserSeckillRecord::getState, ResultCodeConstant.PAY_SUCCESS);
        int row1 = userSeckillRecordMapper.update(null, recordWrapper);
        if (row1 == 0) {
            // 说明 CAS 失败，订单已被超时取消的延迟消息抢占了
            log.info("订单{}状态已变更，跳过支付回调", orderId);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "success";  // 停止支付宝重试
        }
        log.info("支付宝回调处理成功，订单 {} 已支付，交易号: {}", orderId, tradeNo);
        return "success";
    }
}
