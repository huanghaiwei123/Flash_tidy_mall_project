package com.gdou.config;

import org.springframework.amqp.core.*;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE   = "seckill.exchange";
    public static final String QUEUE      = "seckill.queue";
    public static final String ROUTING_KEY = "seckill.routing.key";

    public static final String DEAD_EXCHANGE   = "seckill.dead.exchange";
    public static final String DEAD_QUEUE      = "seckill.dead.queue";
    public static final String DEAD_ROUTING_KEY = "seckill.dead.routing.key";

    public static final String ORDER_CANCEL_DELAY_EXCHANGE   = "order.cancel.delay.exchange";
    public static final String ORDER_CANCEL_DELAY_QUEUE      = "order.cancel.delay.queue";
    public static final String ORDER_CANCEL_DELAY_ROUTING_KEY = "order.cancel.delay.routing.key";

    public static final String ORDER_CANCEL_DEAD_EXCHANGE    = "order.cancel.dead.exchange";
    public static final String ORDER_CANCEL_DEAD_QUEUE       = "order.cancel.dead.queue";
    public static final String ORDER_CANCEL_DEAD_ROUTING_KEY  = "order.cancel.dead.routing.key";

    //    业务交换机
    @Bean
    public DirectExchange SeckillExchange() {
        return new DirectExchange(EXCHANGE);
    }

//    绑定业务队列，绑定死信，消费失败三次后进入死信队列
    @Bean
    public Queue SeckillQueue() {
        return  QueueBuilder.durable(QUEUE)   //持久化队列，mq重启后队列还在
                .deadLetterExchange(DEAD_EXCHANGE)   //绑定死信交换机
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)   //绑定死信路由键
                .build();
    }

//   绑定业务队列到交换机
    @Bean
    public Binding SeckillBinding() {
        return BindingBuilder.bind(SeckillQueue()).to(SeckillExchange()).with(ROUTING_KEY);
    }

//    死信交换机
    @Bean
    public DirectExchange DeadExchange() {
        return new DirectExchange(DEAD_EXCHANGE);
    }

    @Bean
    public  Queue deadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(deadQueue()).to(DeadExchange()).with(DEAD_ROUTING_KEY);
    }
//    json序列化，替代默认的jdk序列化
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

//    延迟交换机
    @Bean
    public DirectExchange orderCancelExchange() {
        return new DirectExchange(ORDER_CANCEL_DELAY_EXCHANGE);
    }

//     延迟队列
    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_DELAY_QUEUE)
                .ttl(15*60*1000)    //15分钟
                .deadLetterExchange(ORDER_CANCEL_DEAD_EXCHANGE )       //绑定死信交换机
                .deadLetterRoutingKey(ORDER_CANCEL_DEAD_ROUTING_KEY)      //绑定死信路由键
                .build();
    }

//    绑定订单取消队列到订单取消交换机
    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(orderCancelExchange()).with(ORDER_CANCEL_DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderCancelDeadExchange() {
        return new DirectExchange(ORDER_CANCEL_DEAD_EXCHANGE);
    }

    @Bean
    public Queue orderCancelDeadQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_DEAD_QUEUE).build();
    }

    @Bean
    public Binding orderCancelDeadBinding() {
        return BindingBuilder.bind(orderCancelDeadQueue()).to(orderCancelDeadExchange()).with(ORDER_CANCEL_DEAD_ROUTING_KEY);
    }

    /**
     * MQ 异步发送线程池，避免 convertAndSend 阻塞请求线程
     */
    @Bean("mqExecutor")
    public ThreadPoolTaskExecutor mqExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(5000);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("mq-async-");
        executor.initialize();
        return executor;
    }
}
