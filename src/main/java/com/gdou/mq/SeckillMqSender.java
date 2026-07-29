package com.gdou.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillMqSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE_NAME = "seckill.exchange";
    private static final String ROUTING_KEY = "seckill.routing.key";
    @Autowired
    private MessageConverter messageConverter;
    public void send(SeckillMessage msg){
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, msg);
        log.info("秒杀消息已发送:{}",msg);
    }

}
