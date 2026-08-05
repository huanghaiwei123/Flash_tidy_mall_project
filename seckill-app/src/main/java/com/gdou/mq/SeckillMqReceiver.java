package com.gdou.mq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gdou.Constant.RedisConstant;
import com.gdou.Constant.ResultCodeConstant;
import com.gdou.mapper.SeckillMapper;
import com.gdou.mapper.SeckillOrderMapper;
import com.gdou.mapper.UserSeckillRecordMapper;
import com.gdou.pojo.entity.Seckill;
import com.gdou.pojo.entity.SeckillOrder;
import com.gdou.pojo.entity.UserSeckillRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
@Component
@Slf4j
public class SeckillMqReceiver {
    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private UserSeckillRecordMapper userSeckillRecordMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private SeckillMqSender seckillMqSender;

    @RabbitListener(queues = "seckill.queue")
    @Transactional
    public void onMessage(SeckillMessage message) {
        Long seckillId = message.getSeckillId();
        String userId = message.getUserId();
        String messageId = message.getMessageId();
        String idempotentKey = RedisConstant.MQ_IDEMPOTENT_PREFIX + ":" + messageId;
        log.info("消费秒杀消息：seckillId={}, userId={}, bucketId={}, messageId={}",
                seckillId, userId, message.getBucketId(), messageId);

        // ==== 幂等检查（SETNX 原子操作）====
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(first)) {
            log.warn("重复消息，跳过 messageId={}", messageId);
            return;
        }

        try {
            // ① DB 扣库存
            Seckill seckill = seckillMapper.selectById(seckillId);
            int count = seckillMapper.deductById(seckillId);
            if (count == 0) {
                // 库存不足是正常业务结果，回滚 Redis 后正常 ACK，不触发重试
                rollbackRedis(seckillId, userId, message.getBucketId());
                log.warn("DB库存不足，已回滚Redis，seckillId={}, userId={}", seckillId, userId);
                return;
            }

            // ② 创建秒杀订单（保存 bucketId 用于后续精确回滚）
            SeckillOrder order = SeckillOrder.builder()
                    .seckillId(seckillId)
                    .userId(Long.valueOf(userId))
                    .goodsName(seckill.getName())
                    .price(seckill.getPrice())
                    .state(ResultCodeConstant.UNPAY)
                    .bucketId(message.getBucketId())
                    .build();
            seckillOrderMapper.insert(order);
            log.info("秒杀订单创建成功 orderId={}", order.getOrderId());

            // ③ 创建用户秒杀记录
            UserSeckillRecord record = UserSeckillRecord.builder()
                    .seckillId(seckillId)
                    .userId(Long.valueOf(userId))
                    .orderId(order.getOrderId())
                    .state(ResultCodeConstant.UNPAY)
                    .build();
            userSeckillRecordMapper.insert(record);

            // ④ 发送 15 分钟超时取消的延迟消息
            SeckillMessage cancelMsg = SeckillMessage.builder()
                    .seckillId(seckillId)
                    .userId(userId)
                    .bucketId(message.getBucketId())
                    .orderId(order.getOrderId())
                    .build();
            seckillMqSender.sendCancelDelay(cancelMsg);

        } catch (Exception e) {
            // 业务失败 → 删除幂等 key，允许 MQ 重试
            redisTemplate.delete(idempotentKey);
            log.warn("消费处理异常，已删除幂等键允许重试 messageId={}", messageId, e);
            throw e;
        }
    }

    @RabbitListener(queues = "seckill.dead.queue")
    public void onDeadMessage(SeckillMessage message) {
        // 检查 Redis 用户记录是否已不存在（说明已被回滚过）
        String userKey = RedisConstant.USER_SECKILL_RECORD + ":{" + message.getSeckillId() + "}:" + message.getUserId();
        if (Boolean.FALSE.equals(redisTemplate.hasKey(userKey))) {
            log.info("死信消息对应的Redis记录已不存在，跳过重复回滚");
            return;
        }
        log.error("进入死信队列 seckillId={}, userId={}, bucketId={}",
                message.getSeckillId(), message.getUserId(), message.getBucketId());
        rollbackRedis(message.getSeckillId(), message.getUserId(), message.getBucketId());
    }

    @RabbitListener(queues = "order.cancel.dead.queue")
    @Transactional
    public void onCancelMessage(SeckillMessage message) {
        Long orderId = message.getOrderId();
        Long seckillId = message.getSeckillId();
        Integer bucketId = message.getBucketId();
        String userId = message.getUserId();
        String messageId = message.getMessageId();

        // ==== 幂等检查 ====
        String idempotentKey = RedisConstant.MQ_IDEMPOTENT_PREFIX + ":" + messageId;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(first)) {
            log.warn("取消消息已处理过，跳过 messageId={}", messageId);
            return;
        }

        try {
            // ① CAS 原子更新订单状态：只有 UNPAY 才改为 PAY_CANCEL
            LambdaUpdateWrapper<SeckillOrder> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(SeckillOrder::getOrderId, orderId)
                   .eq(SeckillOrder::getState, ResultCodeConstant.UNPAY);  // CAS 条件
            wrapper.set(SeckillOrder::getState, ResultCodeConstant.PAY_CANCEL);
            int rows = seckillOrderMapper.update(null, wrapper);

            if (rows == 0) {
                log.info("订单 {} 状态不是未支付，跳过取消", orderId);
                return;
            }
            log.info("订单取消，订单状态 CAS 更新成功 orderId={}", orderId);

            // ② 更新用户秒杀记录
            LambdaUpdateWrapper<UserSeckillRecord> recordWrapper = new LambdaUpdateWrapper<>();
            recordWrapper.eq(UserSeckillRecord::getOrderId, orderId);
            recordWrapper.set(UserSeckillRecord::getState, ResultCodeConstant.PAY_CANCEL);
            userSeckillRecordMapper.update(null, recordWrapper);

            // ③ DB 回补库存
            seckillMapper.incrementById(seckillId);

            // ④ Redis 回补桶库存 + 删除用户标记
            rollbackRedis(seckillId, userId, bucketId);

            log.info("订单超时已取消，orderId={}, seckillId={}, bucketId={}", orderId, seckillId, bucketId);

        } catch (Exception e) {
            // 业务失败 → 删除幂等 key，允许 MQ 重试
            redisTemplate.delete(idempotentKey);
            log.warn("取消处理异常，已删除幂等键允许重试 messageId={}", messageId, e);
            throw e;
        }
    }

    /**
     * 回滚 Redis：桶库存 +1 + 删除用户防重标记
     */
    private void rollbackRedis(Long seckillId, String userId, Integer bucketId) {
        String userKey = RedisConstant.USER_SECKILL_RECORD + ":{" + seckillId + "}:" + userId;
        String stockKey = RedisConstant.SECKILL_STOCK + ":{" + seckillId + "}:" + bucketId;
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(userKey);
    }
}
