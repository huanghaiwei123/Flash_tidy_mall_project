package com.gdou.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.mapper.MqMessageLogMapper;
import com.gdou.mq.MqOrderMessage;
import com.gdou.mq.MqSender;
import com.gdou.pojo.entity.MqMessageLog;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;


@Component
@Slf4j
public class MqRetryTask {
    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;
    @Autowired
    private MqSender mqSender;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private final Integer MAX_RETRY_COUNT=5;
    /**
     * 每隔三十秒扫描一次是否有投递失败的消息
     */
    @Scheduled(fixedRate = 30*1000)
    public void retryTask() {
        try {
            String TASK_MQ_RETRY = "task-mq-retry-";
            MDC.put("traceId", TASK_MQ_RETRY + System.currentTimeMillis());
            LambdaQueryWrapper<MqMessageLog> mqMessageLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
            mqMessageLogLambdaQueryWrapper.ne(MqMessageLog::getStatus, 1)
                    .lt(MqMessageLog::getRetryCount, MAX_RETRY_COUNT)
                    .orderByAsc(MqMessageLog::getCreateTime)
                    .last("limit 100");
            ;
            List<MqMessageLog> mqMessageLogs = mqMessageLogMapper.selectList(mqMessageLogLambdaQueryWrapper);
            for (MqMessageLog mqMessageLog : mqMessageLogs) {
                String exchange = mqMessageLog.getExchange();
                String routingKey = mqMessageLog.getRoutingKey();
                try {
                    MqOrderMessage message = objectMapper.readValue(mqMessageLog.getMessageBody(), MqOrderMessage.class);
                    CorrelationData correlationData = new CorrelationData();
                    correlationData.setId(message.getMessageId());
                    rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
                    LambdaUpdateWrapper<MqMessageLog> mqMessageLogLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
                    mqMessageLogLambdaUpdateWrapper.eq(MqMessageLog::getId, mqMessageLog.getId());
                    mqMessageLogLambdaUpdateWrapper.set(MqMessageLog::getStatus, 0);
                    mqMessageLogLambdaUpdateWrapper.setSql("retry_count = retry_count+1");
                    mqMessageLogLambdaUpdateWrapper.set(MqMessageLog::getUpdateTime, new Date());
                    mqMessageLogMapper.update(mqMessageLogLambdaUpdateWrapper);
                } catch (Exception e) {
                    log.error("消息发送失败，message={},cause={}", mqMessageLog.getId(), e.getMessage());
                }
            }
        }finally {
            MDC.clear();
        }
    }
}
