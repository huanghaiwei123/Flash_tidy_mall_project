package com.gdou.mq;

import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
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
        MessagePostProcessor mpp = message -> {
            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isEmpty()) {
                message.getMessageProperties().setHeader("traceId", traceId);
            }
            return message;
        };

        rabbitTemplate.convertAndSend(exchangeName, routingKey, mqOrderMessage, mpp);
    }

    public void orderDelaySend(String exchangeName, String routingKey, MqOrderDelayMessage mqOrderDelayMessage) {
        MessagePostProcessor mpp = message -> {
            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isEmpty()) {
                message.getMessageProperties().setHeader("traceId", traceId);
            }
            return message;
        };

        rabbitTemplate.convertAndSend(exchangeName, routingKey, mqOrderDelayMessage, mpp);
    }

}