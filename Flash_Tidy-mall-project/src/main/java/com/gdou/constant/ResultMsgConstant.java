package com.gdou.constant;

public class ResultMsgConstant {
    public static final String SUCCESS = "success";
    public static final String FAIL = "fail";
    // ========== 订单状态常量 ==========
    public static final String STATUS_PENDING_PAY = "PENDING_PAY";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_REFUNDING = "REFUNDING";
    public static final String STATUS_REFUNDED = "REFUNDED";
    // ========== 订单明细状态常量 ==========
    public static final String ITEM_STATUS_PENDING = "PENDING";
    public static final String ITEM_STATUS_SHIPPED = "SHIPPED";
    public static final String ITEM_STATUS_RECEIVED = "RECEIVED";
    // ========== 秒杀商品库存key前缀 ==========
    public static final String REDIS_SECKILL_STOCK_PREFIX= "REDIS_SECKILL_STOCK";
    public static final String REDIS_SECKILL_STARTTIME_PREFIX= "REDIS_SECKILL_STARTTIME";
    public static final String REDIS_SECKILL_ENDTIME_PREFIX= "REDIS_SECKILL_ENDTIME";
    public static final String REDIS_SECKILL_USER_PREFIX= "REDIS_SECKILL_USER";
    // ========== 秒杀验证码答案key前缀 ==========
    public static final String REDIS_SECKILL_CAPTCHA_PREFIX= "REDIS_SECKILL_CAPTCHA";
}
