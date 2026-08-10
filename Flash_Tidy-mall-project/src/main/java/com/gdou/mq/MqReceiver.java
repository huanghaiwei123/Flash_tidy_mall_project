package com.gdou.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.common.Result;
import com.gdou.constant.MqConstant;
import com.gdou.constant.ResultMsgConstant;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.*;
import com.gdou.pojo.dto.OrderDto;
import com.gdou.pojo.dto.ShoppingCarDto;
import com.gdou.pojo.entity.*;
import com.gdou.pojo.vo.OrderItemVo;
import com.gdou.pojo.vo.OrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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
    private MqSender mqSender;
    /**
     * 监听订单队列，处理正常订单消息
     */
    @RabbitListener(queues = MqConstant.MQ_ORDER_Queue)
    @Transactional(rollbackFor = Exception.class)
    public void orderHandle(MqOrderMessage orderMessage) {
        System.out.println("收到订单消息: " + orderMessage);
        OrderDto orderDto = orderMessage.getOrderDto();
        Long userId = orderMessage.getUserId();
        String messageId = orderMessage.getMessageId();
        Boolean b = redisTemplate.opsForValue().setIfAbsent(messageId, "1", 1, TimeUnit.DAYS);
        if(Boolean.FALSE.equals(b)) {
            log.warn("该订单消息已经处理过了，不予处理，立即返回");
            return;
        }
        List<ShoppingCarDto> list = orderDto.getCar();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for(ShoppingCarDto shoppingCarDto:list){
            LambdaQueryWrapper<Sku> skuLambdaQueryWrapper = new LambdaQueryWrapper<>();
            skuLambdaQueryWrapper.eq(Sku::getId, shoppingCarDto.getSkuId());
            skuLambdaQueryWrapper.eq(Sku::getSpuId, shoppingCarDto.getSpuId());
            Sku sku = skuMapper.selectOne(skuLambdaQueryWrapper);
            //计算单类商品金额
            totalPrice  = totalPrice.add(sku.getPrice().multiply(BigDecimal.valueOf(shoppingCarDto.getQuantity())));
        }
        Long couponId = orderDto.getCouponId();
        LambdaQueryWrapper<Coupon> couponLambdaQueryWrapper = new LambdaQueryWrapper<>();
        couponLambdaQueryWrapper.eq(Coupon::getId, couponId);
        Coupon coupon = couponMapper.selectOne(couponLambdaQueryWrapper);
        if(coupon==null){
            log.error("优惠卷{}不存在",couponId);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return;
        }
        BigDecimal discountValue = coupon.getDiscountValue();
        BigDecimal minAmount = coupon.getMinAmount();
        int compare = minAmount.compareTo(totalPrice);
        if(compare>0){
            log.warn("当前消费金额{}还不到优惠卷{}的使用门槛",couponId,totalPrice);
        }else{
            totalPrice = totalPrice.subtract(discountValue);
        }
        if(couponId==null){
            compare=0;
        }
        Map<String,Object> map=new HashMap<>();
//        创建OrderVo
        OrderVo orderVo = OrderVo.builder()
                .orderNo(UUID.randomUUID().toString())
                .status(ResultMsgConstant.STATUS_PENDING_PAY)
                .createTime(new Date())
                .addressSnapshot(orderDto.getAddressSnapshot())
                .totalAmount(compare > 0 ? totalPrice : totalPrice.add(discountValue))
                .payAmount(totalPrice)
                .build();
        Order order = new Order();
        BeanUtils.copyProperties(orderVo,order);
        order.setUpdateTime(new Date());
        orderMapper.insert(order);
//        创建OrderItem
        List<OrderItemVo> orderItems = new ArrayList<>();
        for(ShoppingCarDto shoppingCarDto:list){
            Spu spu = spuMapper.selectById(shoppingCarDto.getSpuId());
            Sku sku = skuMapper.selectById(shoppingCarDto.getSkuId());
            OrderItem orderItem = OrderItem.builder()
                    .orderId(order.getId())
                    .skuId(shoppingCarDto.getSkuId())
                    .spuId(shoppingCarDto.getSpuId())
                    .quantity(shoppingCarDto.getQuantity())
                    .createTime(new Date())
                    .orderNo(order.getOrderNo())
                    .skuName(sku.getName())
                    .skuSpec(sku.getSpec())
                    .skuPrice(sku.getPrice())
                    .skuImage(sku.getImage())
                    .totalPrice(sku.getPrice().multiply(BigDecimal.valueOf(shoppingCarDto.getQuantity())))
                    .build();
            orderItemMapper.insert(orderItem);
            OrderItemVo orderItemVo = new OrderItemVo();
            BeanUtils.copyProperties(orderItem,orderItemVo);
            orderItems.add(orderItemVo);
        }
        map.put("orderVo",orderVo);
        map.put("orderItems",orderItems);
        MqOrderDelayMessage message = new MqOrderDelayMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setOrderDto(orderDto);
        message.setUserId(userId);
        message.setOrderNo(order.getOrderNo());
        mqSender.orderDelaySend(MqConstant.MQ_ORDER_DELAY_EXCHANGE,MqConstant.MQ_ORDER_DELAY_ROUTING_KEY,message);
        // 业务处理逻辑...
        log.info("订单创建成功,{}",map);
    }

    /**
     * 监听死信队列，处理异常订单消息
     */
    @RabbitListener(queues = MqConstant.MQ_ORDER_DEAD_QUEUE)
    public void deadOrderHandle(String message) {
        System.out.println("收到死信订单消息: " + message);
        // 记录日志、告警、人工处理...
    }

    /**
     * 延迟队列，用户下单后十五分钟检查是否付款，不是的话取消订单
     * @param message
     */
    @RabbitListener(queues = MqConstant.MQ_ORDER_DELAY_QUEUE)
    public void delayOrderHandle(MqOrderDelayMessage message) {
        String messageId = message.getMessageId();
        Boolean b = redisTemplate.opsForValue().setIfAbsent(messageId, "1", 1, TimeUnit.DAYS);
        if(Boolean.FALSE.equals(b)) {
            log.warn("这个消息已经处理过了，不予处理");
        }
        LambdaQueryWrapper<Order> orderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderLambdaQueryWrapper.eq(Order::getUserId, message.getUserId());
        orderLambdaQueryWrapper.eq(Order::getOrderNo,message.getOrderNo());
        Order order = orderMapper.selectOne(orderLambdaQueryWrapper);
        if(order==null){
            log.warn("订单{}不存在",message.getOrderNo());
        }
        if(!order.getStatus().equals(ResultMsgConstant.STATUS_COMPLETED)){
            log.info("订单状态不是未支付，无法取消");
        }
        //更新订单状态为未支付
        order.setStatus(ResultMsgConstant.STATUS_COMPLETED);
        orderMapper.updateById(order);
        log.info("十五分钟内用户未支付，订单已取消");
    }
}