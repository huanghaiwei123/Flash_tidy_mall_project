package com.gdou.mq;

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
    @RabbitListener(queues = "seckill.queue")
    @Transactional
    public void onMessage(SeckillMessage message) {
        Long seckillId = message.getSeckillId();
        String userId = message.getUserId();
        log.info("消费秒杀消息：seckillId={}, userId={}", seckillId, userId);

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

        // ③ 创建秒杀记录
        UserSeckillRecord record = UserSeckillRecord.builder()
                .seckillId(seckillId)
                .userId(Long.valueOf(userId))
                .orderId(order.getOrderId())
                .state(ResultCodeConstant.UNPAY)
                .build();
        userSeckillRecordMapper.insert(record);
        log.info("秒杀订单创建成功 orderId={}", order.getOrderId());
    }

    @RabbitListener(queues = "seckill.dead.queue")
    public void onDeadMessage(SeckillMessage message) {
        Long seckillId = message.getSeckillId();
        String userId = message.getUserId();
        String stockKey= RedisConstant.SECKILL_STOCK+seckillId;
        String userSeckillRecordKey = RedisConstant.USER_SECKILL_RECORD+":"+userId+":"+seckillId;
//        redis库存回滚
        redisTemplate.opsForValue().increment(stockKey);
//        删除用户下单标记
        redisTemplate.delete(userSeckillRecordKey);
        log.error("秒杀最终失败已回滚Redis，seckillId={}, userId={}", seckillId, userId);
    }
}
