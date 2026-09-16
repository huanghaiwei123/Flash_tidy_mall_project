package com.gdou.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.constant.MqConstant;
import com.gdou.mapper.MqMessageLogMapper;
import com.gdou.pojo.entity.MqMessageLog;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Slf4j
public class RabbitMqConfig  {
    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;
    /**
     * 订单交易处理交换机
     * @return
     */
   @Bean
    public DirectExchange orderExchange(){
       return new DirectExchange(MqConstant.MQ_ORDER_EXCHANGE);
   }

    /**
     * 订单交易处理队列
     * @return
     */
   @Bean
    public Queue orderQueue(){
       return QueueBuilder.durable(MqConstant.MQ_ORDER_Queue)
               .deadLetterExchange(MqConstant.MQ_ORDER_DEAD_EXCHANGE)
               .deadLetterRoutingKey(MqConstant.MQ_ORDER_DEAD_ROUTING_KEY)
               .build();
   }

   /**
     * 订单交易处理绑定（队列绑定到交换机，指定routing key）
     * @return
     */
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with(MqConstant.MQ_ORDER_ROUTING_KEY);
    }

    /**
     * 订单交易死信交换机
     * @return
     */
    @Bean
    public DirectExchange OrderDeadExchange(){
        return new DirectExchange(MqConstant.MQ_ORDER_DEAD_EXCHANGE);
    }

    /**
     * 订单交易死信处理队列
     * @return
     */
    @Bean 
    public Queue OrderDeadQueue(){
        return QueueBuilder.durable(MqConstant.MQ_ORDER_DEAD_QUEUE).build();
    }

    /**
     * 订单交易死信队列与交换机绑定
     * @return
     */
    @Bean
    public Binding OrderDeadBinding() {
        return BindingBuilder.bind(OrderDeadQueue()).to(OrderDeadExchange()).with(MqConstant.MQ_ORDER_DEAD_ROUTING_KEY);
    }

    /**
     * 订单十五分钟取消延迟交换机
     * @return
     */
    @Bean
    public DirectExchange orderDelayCancelExchange(){
        return new DirectExchange(MqConstant.MQ_ORDER_DELAY_EXCHANGE);
    }

    /**
     * 订单延迟取消队列（带 TTL，消息过期后转发到消费队列）
     * @return
     */
    @Bean
    public Queue orderDelayCancelQueue(){
        return QueueBuilder.durable(MqConstant.MQ_ORDER_DELAY_QUEUE)
                .ttl(15 * 60 * 1000)
                .deadLetterExchange(MqConstant.MQ_ORDER_DELAY_EXCHANGE)
                .deadLetterRoutingKey(MqConstant.MQ_ORDER_DELAY_CONSUME_ROUTING_KEY)
                .build();
    }

    /**
     * 订单延迟取消绑定（发送到延迟队列）
     * @return
     */
    @Bean
    public Binding orderDelayCancelBinding() {
        return BindingBuilder.bind(orderDelayCancelQueue()).to(orderDelayCancelExchange()).with(MqConstant.MQ_ORDER_DELAY_ROUTING_KEY);
    }

    /**
     * 延迟取消消费队列（消息过期后实际消费的队列）
     */
    @Bean
    public Queue orderDelayConsumeQueue() {
        return QueueBuilder.durable(MqConstant.MQ_ORDER_DELAY_CONSUME_QUEUE).build();
    }

    /**
     * 延迟消费队列绑定（过期消息从死信交换机路由到消费队列）
     */
    @Bean
    public Binding orderDelayConsumeBinding() {
        return BindingBuilder.bind(orderDelayConsumeQueue()).to(orderDelayCancelExchange()).with(MqConstant.MQ_ORDER_DELAY_CONSUME_ROUTING_KEY);
    }


    /**
     * JSON 消息转换器：对象自动序列化为 JSON，无需实现 Serializable
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     *消息队列异步处理线程池
     * @return
     */
    @Bean("mq_executor")
    public ThreadPoolTaskExecutor mqExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mq-sender-");  // 底层自动用 CustomizableThreadFactory
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        executor.setTaskDecorator(runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();  //取主线程MDC
            return ()->{
                try{
                    if(context!=null){
                        MDC.setContextMap(context);   //塞进子线程
                    }
                    runnable.run();
                }finally {
                    MDC.clear();   //任务结束清掉，避免线程池里的线程被污染
                }
            };
        });
        return executor;
    }

    @Bean
    public  RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        //        消息发送到交换机的确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if(correlationData==null){
                return;
            }
            String messageId = correlationData.getId();
            LambdaQueryWrapper<MqMessageLog> mqMessageLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
            mqMessageLogLambdaQueryWrapper.eq(MqMessageLog::getMessageId, messageId);
            MqMessageLog mqMessageLog = mqMessageLogMapper.selectOne(mqMessageLogLambdaQueryWrapper);
            if(mqMessageLog==null){
                return;
            }
            if (ack) {
                mqMessageLog.setStatus(1);  //消息发送成功
                log.info("消息发送成功，messageId={}",correlationData!=null?correlationData.getId():"null");
            }else{
                mqMessageLog.setStatus(2);
                log.info("消息发送失败，messageId={}，cause={}",correlationData!=null?correlationData.getId():"null",cause);
            }
            mqMessageLog.setUpdateTime(new Date());
            mqMessageLogMapper.updateById(mqMessageLog);
        });

//        消息路由不到队列时的回调，指的是交换机存在但是routingKey不存在的时候
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.info("消息路由失败，message={},replyCode={},replyText={},exchange={},routingKey={}",message,replyCode,replyText,exchange,routingKey);
        });
        return rabbitTemplate;
    }

}