package com.gdou.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
    private MessageConverter messageConverter;
    @Async("mqExecutor")
    public void send(SeckillMessage msg){
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, msg);
        log.info("秒杀消息已发送:{}",msg);
    }

    public void sendCancelDelay(SeckillMessage msg){
        rabbitTemplate.convertAndSend(CANCEL_DELAY_EXCHANGE_NAME,CANCEL_DELAY_ROUTING_KEY, msg);
        log.info("订单超时取消延迟消息已发送，orderId={}", msg.getOrderId());
    }

}
