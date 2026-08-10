package com.gdou.config;

import com.gdou.constant.MqConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class RabbitMqConfig  {
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
       Queue queue = QueueBuilder.durable()
               .deadLetterExchange(MqConstant.MQ_ORDER_DEAD_EXCHANGE)
               .deadLetterRoutingKey(MqConstant.MQ_ORDER_DEAD_ROUTING_KEY)
               .build();
       return queue;
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
        return QueueBuilder.durable().build();
    }

    /**
     * 订单交易死信队列与交换机绑定
     * @return
     */
    @Bean
    public Binding OrderDeadBinding() {
        return BindingBuilder.bind(orderQueue()).to(OrderDeadExchange()).with(MqConstant.MQ_ORDER_DEAD_ROUTING_KEY);
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
     * 订单延迟取消队列
     * @return
     */
    @Bean
    public Queue orderDelayCancelQueue(){
        return QueueBuilder.durable().ttl(15*60*1000).build();
    }

    /**
     * 订单延迟取消绑定
     * @return
     */
    @Bean
    public Binding orderDelayCancelBinding() {
        return BindingBuilder.bind(orderDelayCancelQueue()).to(orderDelayCancelExchange()).with(MqConstant.MQ_ORDER_DELAY_ROUTING_KEY);
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
        return executor;
    }
}