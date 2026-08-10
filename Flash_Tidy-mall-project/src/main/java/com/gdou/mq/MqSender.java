package com.gdou.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MqSender{
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Async("mq_executor")
    public void orderSend(String exchangeName, String routingKey, MqOrderMessage mqOrderMessage) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, mqOrderMessage);
    }

    public void orderDelaySend(String exchangeName, String routingKey, MqOrderDelayMessage mqOrderDelayMessage) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, mqOrderDelayMessage);
    }

}