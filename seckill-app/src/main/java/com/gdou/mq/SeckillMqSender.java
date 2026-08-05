package com.gdou.mq;

import com.gdou.Constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class SeckillMqSender {
    private static final String EXCHANGE_NAME = "seckill.exchange";
    private static final String ROUTING_KEY = "seckill.routing.key";
    private static final String CANCEL_DELAY_EXCHANGE_NAME = "order.cancel.delay.exchange";
    private static final String CANCEL_DELAY_ROUTING_KEY = "order.cancel.delay.routing.key";

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Async("mqExecutor")
    public void send(SeckillMessage msg) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        msg.setMessageId(messageId);

        SeckillCorrelationData cd = new SeckillCorrelationData(messageId, msg);
        try {
            // msg 是发给消费者的消息体，cd 是只给 ConfirmCallback 的元数据
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, msg, cd);
            log.info("秒杀消息已发送 messageId={}", messageId);
        } catch (Exception e) {
            log.error("消息发送失败(broker不可达) messageId={}", messageId, e);
            rollbackRedis(msg);
        }
    }

    public void sendCancelDelay(SeckillMessage msg) {
        String messageId = UUID.randomUUID().toString().replace("-", "");
        msg.setMessageId(messageId);

        SeckillCorrelationData cd = new SeckillCorrelationData(messageId, msg);
        rabbitTemplate.convertAndSend(CANCEL_DELAY_EXCHANGE_NAME, CANCEL_DELAY_ROUTING_KEY, msg, cd);
        log.info("订单超时取消延迟消息已发送 messageId={}, orderId={}", messageId, msg.getOrderId());
    }

    /**
     * 回滚 Redis：桶库存 +1 + 删除用户防重标记
     */
    private void rollbackRedis(SeckillMessage msg) {
        String stockKey = RedisConstant.SECKILL_STOCK + ":{" + msg.getSeckillId() + "}:" + msg.getBucketId();
        String userKey = RedisConstant.USER_SECKILL_RECORD + ":{" + msg.getSeckillId() + "}:" + msg.getUserId();
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.delete(userKey);
        log.info("已回滚Redis(发送异常) seckillId={} bucketId={}", msg.getSeckillId(), msg.getBucketId());
    }
}
