package com.gdou.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gdou.constant.MqConstant;
import com.gdou.constant.ResultMsgConstant;
import com.gdou.mapper.*;
import com.gdou.pojo.dto.OrderDto;
import com.gdou.pojo.dto.ShoppingCarDto;
import com.gdou.pojo.entity.*;
import com.gdou.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.interceptor.TransactionAspectSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class MqReceiver {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ShoppingCarMapper shoppingCarMapper;
    @Autowired
    private MqSender mqSender;
    @Autowired
    private OrderService orderService;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 监听订单队列，处理正常订单消息
     */
    @RabbitListener(queues = MqConstant.MQ_ORDER_Queue)
    @Transactional(rollbackFor = Exception.class)
    public void orderHandle(MqOrderMessage orderMessage,
                            @Header(name = "traceId", required = false) String traceId) {
        MDC.put("traceId", traceId);
        log.info("收到订单消息: " + orderMessage);
        int compare=0;
        Long userId = orderMessage.getUserId();
        OrderDto orderDto = orderMessage.getOrderDto();
        String messageId = orderMessage.getMessageId();
        try {
            // 幂等检查：防止重复消费
            Boolean b = redisTemplate.opsForValue().setIfAbsent(messageId, "1", 1, TimeUnit.DAYS);
            if (Boolean.FALSE.equals(b)) {
                log.warn("该订单消息已经处理过了，不予处理，立即返回");
                return;
            }
            List<ShoppingCarDto> list = orderDto.getCar();
            BigDecimal totalPrice = BigDecimal.ZERO;
            // 秒杀订单使用秒杀价格
            boolean isSeckill = "SECKILL".equals(orderMessage.getOrderType());
            // 一次查询，缓存 SKU，避免循环内重复查
            Map<Long, Sku> skuCache = new HashMap<>();
            for (ShoppingCarDto shoppingCarDto : list) {
                LambdaQueryWrapper<Sku> skuLambdaQueryWrapper = new LambdaQueryWrapper<>();
                skuLambdaQueryWrapper.eq(Sku::getId, shoppingCarDto.getSkuId());
                skuLambdaQueryWrapper.eq(Sku::getSpuId, shoppingCarDto.getSpuId());
                Sku sku = skuMapper.selectOne(skuLambdaQueryWrapper);
                skuCache.put(shoppingCarDto.getSkuId(), sku);
                // 秒杀订单用秒杀价，普通订单用售价
                BigDecimal unitPrice = (isSeckill && sku.getSeckillPrice() != null)
                        ? sku.getSeckillPrice() : sku.getPrice();
                totalPrice = totalPrice.add(unitPrice.multiply(BigDecimal.valueOf(shoppingCarDto.getQuantity())));
            }
            Long couponId = orderDto.getCouponId();
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (couponId != null) {
                LambdaQueryWrapper<Coupon> couponLambdaQueryWrapper = new LambdaQueryWrapper<>();
                couponLambdaQueryWrapper.eq(Coupon::getId, couponId);
                Coupon coupon = couponMapper.selectOne(couponLambdaQueryWrapper);
                if (coupon == null) {
                    log.error("用户{}使用的优惠卷id被篡改,此次订单无效", userId);
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return;
                }
                BigDecimal discountValue = coupon.getDiscountValue();
                BigDecimal minAmount = coupon.getMinAmount();
                if (minAmount.compareTo(totalPrice) > 0) {
                    log.warn("当前消费金额{}还不到优惠卷{}的使用门槛{}", couponId, totalPrice, minAmount);
                } else {
                    discountAmount = discountValue;
                    totalPrice = totalPrice.subtract(discountValue);
                    compare += 1;
                }
            } else {
                log.info("用户{}不使用优惠卷", userId);
            }
            // 创建 Order
            Order order = Order.builder()
                    .orderNo(UUID.randomUUID().toString())
                    .userId(userId)
                    .status(ResultMsgConstant.STATUS_PENDING_PAY)
                    .createTime(new Date())
                    .addressSnapshot(objectMapper.writeValueAsString(orderDto.getAddressSnapshot()))
                    .totalAmount(compare > 0 ? totalPrice.add(discountAmount) : totalPrice)
                    .discountAmount(discountAmount)
                    .payAmount(totalPrice)
                    .orderType(orderMessage.getOrderType())
                    .updateTime(new Date())
                    .build();
            orderMapper.insert(order);
            // 创建OrderItem
            for (ShoppingCarDto shoppingCarDto : list) {
                Long skuId = shoppingCarDto.getSkuId();
                Sku sku = skuCache.get(skuId);
                BigDecimal itemUnitPrice = (isSeckill && sku.getSeckillPrice() != null)
                        ? sku.getSeckillPrice() : sku.getPrice();
                OrderItem orderItem = OrderItem.builder()
                        .orderId(order.getId())
                        .skuId(skuId)
                        .spuId(shoppingCarDto.getSpuId())
                        .merchantId(shoppingCarDto.getMerchantId())
                        .quantity(shoppingCarDto.getQuantity())
                        .createTime(new Date())
                        .orderNo(order.getOrderNo())
                        .skuName(sku.getName())
                        .skuSpec(sku.getSpec())
                        .skuPrice(itemUnitPrice)
                        .skuImage(sku.getImage())
                        .totalPrice(itemUnitPrice.multiply(BigDecimal.valueOf(shoppingCarDto.getQuantity())))
                        .itemStatus(ResultMsgConstant.ITEM_STATUS_PENDING)
                        .build();
                // 扣减可售库存 + 增加锁定库存（一次 setSql，避免覆盖）
                LambdaUpdateWrapper<Sku> skuLambdaQueryWrapper = new LambdaUpdateWrapper<>();
                skuLambdaQueryWrapper.eq(Sku::getId, skuId);
                skuLambdaQueryWrapper.eq(Sku::getSpuId, shoppingCarDto.getSpuId());
                skuLambdaQueryWrapper.setSql("available_stock = available_stock - 1, locked_stock = locked_stock + 1"
                        .replace("1", String.valueOf(shoppingCarDto.getQuantity())));
                skuMapper.update(null, skuLambdaQueryWrapper);
                orderItemMapper.insert(orderItem);
            }
            MqOrderDelayMessage message = new MqOrderDelayMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setUserId(userId);
            message.setOrderNo(order.getOrderNo());
            // 清理购物车中已下单的商品
            LambdaQueryWrapper<ShoppingCar> carWrapper = new LambdaQueryWrapper<>();
            carWrapper.eq(ShoppingCar::getUserId, userId);
            shoppingCarMapper.delete(carWrapper);
            mqSender.orderDelaySend(MqConstant.MQ_ORDER_DELAY_EXCHANGE, MqConstant.MQ_ORDER_DELAY_ROUTING_KEY, message);
            log.info("订单{}创建成功，共{}件商品，实付{}元", order.getOrderNo(), list.size(), order.getPayAmount());
        } catch (Exception e) {
            // DB 回滚后删除 Redis 幂等 key，允许消息重试
            redisTemplate.delete(messageId);
            log.warn("订单处理失败，已清除幂等标记，等待重试: {}", e.getMessage());
            ShoppingCarDto carDto = orderDto.getCar().get(0);
            Long skuId = carDto.getSkuId();
            String stockKey=ResultMsgConstant.REDIS_SECKILL_STOCK_PREFIX+":"+skuId;
            if(orderMessage.getOrderType().equals("SECKILL")){
                redisTemplate.opsForValue().increment(stockKey);
            }
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        } finally {
            MDC.clear();
        }
    }

    /**
     * 监听死信队列，处理异常订单消息
     */
    @RabbitListener(queues = MqConstant.MQ_ORDER_DEAD_QUEUE)
    public void deadOrderHandle(String message,
                                @Header(name = "traceId", required = false) String traceId) {
        MDC.put("traceId", traceId);
        try {
            log.info("收到死信订单消息: " + message);
            // 记录日志、告警、人工处理...
        } finally {
            MDC.clear();
        }
    }

    /**
     * 延迟队列，用户下单后十五分钟检查是否付款，不是的话取消订单
     * @param message
     */
    @RabbitListener(queues = MqConstant.MQ_ORDER_DELAY_CONSUME_QUEUE)
    public void delayOrderHandle(MqOrderDelayMessage message,
                                 @Header(name = "traceId", required = false) String traceId) {
        MDC.put("traceId", traceId);
        try {
            String messageId = message.getMessageId();
            Boolean b = redisTemplate.opsForValue().setIfAbsent(messageId, "1", 1, TimeUnit.DAYS);
            if (Boolean.FALSE.equals(b)) {
                log.warn("这个消息已经处理过了，不予处理");
                return;
            }
            LambdaQueryWrapper<Order> orderLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderLambdaQueryWrapper.eq(Order::getUserId, message.getUserId());
            orderLambdaQueryWrapper.eq(Order::getOrderNo, message.getOrderNo());
            Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
            if (order == null) {
                log.warn("订单{}不存在", message.getOrderNo());
                return;
            }
            if (!order.getStatus().equals(ResultMsgConstant.STATUS_PENDING_PAY)) {
                log.info("订单{}状态不是待支付({}),无法取消", message.getOrderNo(), order.getStatus());
                return;
            }
            // 恢复锁定库存 + 更新订单状态为取消
            orderService.restoreStock(message.getOrderNo());
            order.setStatus(ResultMsgConstant.STATUS_CANCELLED);
            orderMapper.updateById(order);
            log.info("订单{}十五分钟内用户未支付，已取消并恢复库存", message.getOrderNo());
        } finally {
            MDC.clear();
        }
    }
}