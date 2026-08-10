package com.gdou.constant;

public class MqConstant {
    public static final String MQ_ORDER_EXCHANGE = "mq_order_exchange";
    public static final String MQ_ORDER_Queue = "mq_order_queue";
    public static final String MQ_ORDER_ROUTING_KEY = "order_routing_key";


    public static final String MQ_ORDER_DEAD_EXCHANGE = "mq_order_dead_exchange";
    public static final String MQ_ORDER_DEAD_QUEUE = "mq_order_dead_queue";
    public static final String MQ_ORDER_DEAD_ROUTING_KEY = "order_dead_routing_key";

    public static final String MQ_ORDER_DELAY_EXCHANGE = "mq_order_delay_exchange";
    public static final String MQ_ORDER_DELAY_QUEUE = "mq_order_delay_queue";
    public static final String MQ_ORDER_DELAY_ROUTING_KEY = "order_delay_routing_key";
    // 延迟消息过期后进入此队列，由 consumer 监听处理
    public static final String MQ_ORDER_DELAY_CONSUME_QUEUE = "mq_order_delay_consume_queue";
    public static final String MQ_ORDER_DELAY_CONSUME_ROUTING_KEY = "order_delay_consume_routing_key";
}