package com.gdou.controller;

import org.springframework.amqp.core.*;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE   = "seckill.exchange";
    public static final String QUEUE      = "seckill.queue";
    public static final String ROUTING_KEY = "seckill.routing.key";

    public static final String DEAD_EXCHANGE   = "seckill.dead.exchange";
    public static final String DEAD_QUEUE      = "seckill.dead.queue";
    public static final String DEAD_ROUTING_KEY = "seckill.dead.routing.key";

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
}
