package com.gdou.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.Constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * 这一部分是生产者给broker发送消息，broker宕机或broker处理消息失败的兜底机制
 */
@Component
@Slf4j
public class MqConfirmFallBack implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);    //broker收到消息，不管消息有没有到达交换机，将当前类设为ConfirmCallback，回调confirm方法
        rabbitTemplate.setReturnsCallback(this);       //broker收到消息，不管消息有没有路由到队列，将当前类设为ReturnCallback，回调returnedMessage方法
        rabbitTemplate.setMandatory(true);    // 触发 ReturnsCallback 的前提
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if(ack){
            return;
        }//正常路径，不做任何事
        log.error("id为{}的消息无法被broker处理，原因为:{}",correlationData!=null?correlationData.getId():"unknown",cause);
        if(correlationData instanceof SeckillCorrelationData){
            SeckillMessage seckillMessage = ((SeckillCorrelationData) correlationData).getSeckillMessage();
            rollBackRedis(seckillMessage);
        }
    }

    @Override
    public void returnedMessage(ReturnedMessage returned) {
        log.error("broker未收到消费者发来的消息，请检查：exchange:{},routingkey:{}",returned.getExchange(),returned.getRoutingKey());
        try {
            SeckillMessage message = objectMapper.readValue(returned.getMessage().getBody(), SeckillMessage.class);
            rollBackRedis(message);
        } catch (IOException e) {
            log.error("解析退回消息失败");
            e.printStackTrace();
        }
    }

    /**
     * 在生产者发送消息失败时用于回滚redis库存
     * @param msg
     */
    private void rollBackRedis(SeckillMessage msg) {
        String stockKey = RedisConstant.SECKILL_STOCK + ":{" + msg.getSeckillId() + "}:" + msg.getBucketId();
        String userKey = RedisConstant.USER_SECKILL_RECORD + ":{" + msg.getSeckillId() + "}:" + msg.getUserId();
        redisTemplate.opsForValue().increment(stockKey);  // 库存 +1
        redisTemplate.delete(userKey);                     // 删除防重标记
        log.info("已回滚Redis(消息失败补偿) seckillId={} bucketId={}", msg.getSeckillId(), msg.getBucketId());
    }
}
