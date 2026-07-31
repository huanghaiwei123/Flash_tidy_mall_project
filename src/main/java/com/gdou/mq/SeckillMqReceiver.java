package com.gdou.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        log.info("消费秒杀消息：seckillId={}, userId={}, bucketId={}", seckillId, userId, message.getBucketId());

        // ① DB 扣库存
        Seckill seckill = seckillMapper.selectById(seckillId);
        int count = seckillMapper.deductById(seckillId);
        if (count == 0) {
            throw new RuntimeException("库存不足");
        }

        // ② 创建秒杀订单
        SeckillOrder order = SeckillOrder.builder()
                .seckillId(seckillId)
                .userId(Long.valueOf(userId))
                .goodsName(seckill.getName())
                .price(seckill.getPrice())
                .state(ResultCodeConstant.UNPAY)
                .build();
        seckillOrderMapper.insert(order);
        log.info("秒杀订单创建成功 orderId={}", order.getOrderId());
   // ④ 发送 15 分钟超时取消的延迟消息
        SeckillMessage cancelMsg = SeckillMessage.builder()
                .seckillId(seckillId)
                .userId(userId)
                .bucketId(message.getBucketId())
                .orderId(order.getOrderId())
                .build();
        seckillMqSender.sendCancelDelay(cancelMsg);
    }

    @RabbitListener(queues = "seckill.dead.queue")
    public void onDeadMessage(SeckillMessage message) {
        Long seckillId = message.getSeckillId();
        String userId = message.getUserId();
        Integer bucketId = message.getBucketId();

        String userSeckillRecordKey = RedisConstant.USER_SECKILL_RECORD + ":{" + seckillId + "}:" + userId;
        // 回滚精确到具体桶
        String bucketStockKey = RedisConstant.SECKILL_STOCK + ":{" + seckillId + "}:" + bucketId;
        redisTemplate.opsForValue().increment(bucketStockKey);
        // 删除用户下单标记
        redisTemplate.delete(userSeckillRecordKey);
        log.error("秒杀最终失败已回滚Redis，seckillId={}, userId={}, bucketId={}", seckillId, userId, bucketId);
    }

    @RabbitListener(queues = "order.cancel.dead.queue")
    @Transactional
    public void onCancelMessage(SeckillMessage message) {
        Long orderId = message.getOrderId();
        Long seckillId = message.getSeckillId();
        Integer bucketId = message.getBucketId();
        String userId = message.getUserId();

        // ① 查订单，只有 UNPAY 才取消
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null || !order.getState().equals(ResultCodeConstant.UNPAY)) {
            log.info("订单 {} 状态不是未支付，跳过取消", orderId);
            return;
        }

        // ② 更新订单状态
        order.setState(ResultCodeConstant.PAY_CANCEL);
        seckillOrderMapper.updateById(order);

        // ③ 更新用户秒杀记录
        LambdaQueryWrapper<UserSeckillRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSeckillRecord::getOrderId, orderId);
        UserSeckillRecord record = userSeckillRecordMapper.selectOne(wrapper);
        if (record != null) {
            record.setState(ResultCodeConstant.PAY_CANCEL);
            userSeckillRecordMapper.updateById(record);
        }

        // ④ DB 回补库存
        seckillMapper.incrementById(seckillId);

        // ⑤ Redis 回补桶库存 + 删除用户标记
        String bucketStockKey = RedisConstant.SECKILL_STOCK + ":{" + seckillId + "}:" + bucketId;
        redisTemplate.opsForValue().increment(bucketStockKey);
        String userSeckillRecordKey = RedisConstant.USER_SECKILL_RECORD + ":{" + seckillId + "}:" + userId;
        redisTemplate.delete(userSeckillRecordKey);

        log.info("订单超时已取消，orderId={}, seckillId={}, bucketId={}", orderId, seckillId, bucketId);
    }



}
