package com.gdou.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.mapper.MqMessageLogMapper;
import com.gdou.pojo.entity.MqMessageLog;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MqSender{
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Async("mq_executor")
    public void orderSend(String exchangeName, String routingKey, MqOrderMessage mqOrderMessage) {
        // 1. 先落库，记录待发送状态
        MqMessageLog mqMessageLog = new MqMessageLog();
        mqMessageLog.setMessageId(mqOrderMessage.getMessageId());
        mqMessageLog.setExchange(exchangeName);
        mqMessageLog.setRoutingKey(routingKey);
        try {
            mqMessageLog.setMessageBody(objectMapper.writeValueAsString(mqOrderMessage));
        } catch (Exception e) {
            throw new RuntimeException("消息序列化失败", e);
        }
        mqMessageLog.setStatus(0);
        mqMessageLog.setRetryCount(0);
        mqMessageLog.setCreateTime(new Date());
        mqMessageLog.setUpdateTime(new Date());
        mqMessageLogMapper.insert(mqMessageLog);

        // 2. 发送，带上 CorrelationData（messageId 用于确认回调对号）
        CorrelationData correlationData = new CorrelationData(mqOrderMessage.getMessageId());
        MessagePostProcessor mpp = message -> {
            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isEmpty()) {
                message.getMessageProperties().setHeader("traceId", traceId);
            }
            return message;
        };

        rabbitTemplate.convertAndSend(exchangeName, routingKey, mqOrderMessage, mpp,correlationData);
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