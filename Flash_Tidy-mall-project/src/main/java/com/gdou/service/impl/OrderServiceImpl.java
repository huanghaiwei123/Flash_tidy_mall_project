package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.constant.MqConstant;
import com.gdou.constant.ResultMsgConstant;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.*;
import com.gdou.mq.MqOrderMessage;
import com.gdou.mq.MqReceiver;
import com.gdou.mq.MqSender;
import com.gdou.pojo.dto.OrderDto;
import com.gdou.pojo.dto.ShoppingCarDto;
import com.gdou.pojo.entity.*;
import com.gdou.pojo.vo.OrderItemVo;
import com.gdou.pojo.vo.OrderVo;
import com.gdou.service.CategoryService;
import com.gdou.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author huanghaiwei
* @description 针对表【order(统一订单表)】的数据库操作Service实现
* @createDate 2026-08-06 21:59:46
*/
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
    implements OrderService{
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private MqSender mqSender;
    /**
     * 用户查询
     */
    @Override
    public Result orderQueryByUser(Long userId,String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Long l = orderMapper.selectCount(wrapper);
        Order order = orderMapper.selectOne(wrapper);
        if(l==0){
            return Result.Fail("订单编号为{}的订单不存在", orderNo);
        }
        LambdaQueryWrapper<OrderItem> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(OrderItem::getOrderNo, orderNo);
        List<OrderItem> orderItems = orderItemMapper.selectList(wrapper1);
        List<OrderItemVo> orderItemVos = new ArrayList<>();
        for(OrderItem orderItem:orderItems){
            OrderItemVo orderItemVo = new OrderItemVo();
            BeanUtils.copyProperties(orderItem,orderItemVo);
            orderItemVos.add(orderItemVo);
        }
        OrderVo orderVo = new OrderVo();
        BeanUtils.copyProperties(order, orderVo);
        Map<Object,Object> map=new HashMap<>();
        map.put("orderVo",orderVo);
        map.put("orderItemVos",orderItemVos);
        return Result.success("订单查询成功,返回订单和订单明细", map);
    }

    /**
     * 商家查询
     * @param merchantId
     * @param orderNo
     * @return
     */
    public Result orderQueryByMerchant(Long merchantId, String orderNo) {
        // 通过 order_item 校验该订单是否有属于此商家的明细
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderNo, orderNo);
        itemWrapper.eq(OrderItem::getMerchantId, merchantId);
        Long itemCount = orderItemMapper.selectCount(itemWrapper);
        if (itemCount == 0) {
            return Result.Fail("订单编号为{}的订单不存在或不属于该商家", orderNo);
        }
        // 查订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(wrapper);
        if (order == null) {
            return Result.Fail("订单编号为{}的订单不存在", orderNo);
        }
        // 只返回该商家的明细（不是整个订单的所有明细）
        List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);
        List<OrderItemVo> orderItemVos = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            OrderItemVo orderItemVo = new OrderItemVo();
            BeanUtils.copyProperties(orderItem, orderItemVo);
            orderItemVos.add(orderItemVo);
        }
        OrderVo orderVo = new OrderVo();
        BeanUtils.copyProperties(order, orderVo);
        Map<Object, Object> map = new HashMap<>();
        map.put("orderVo", orderVo);
        map.put("orderItemVos", orderItemVos);
        return Result.success("订单查询成功", map);
    }

    @Override
    public Result orderQueryListByUser(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            log.info("用户{}暂无订单", userId);
            return Result.success("订单查询成功", Collections.emptyList());
        }
        // 为每个订单组装详情（商品明细 + 店铺名）
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Order order : orders) {
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            List<OrderItemVo> itemVos = new ArrayList<>();
            for (OrderItem item : items) {
                OrderItemVo vo = new OrderItemVo();
                BeanUtils.copyProperties(item, vo);
                vo.setMerchantId(item.getMerchantId());
                // 查店铺名
                if (item.getMerchantId() != null) {
                    Merchant merchant = merchantMapper.selectById(item.getMerchantId());
                    vo.setMerchantName(merchant != null ? merchant.getShopName() : null);
                }
                itemVos.add(vo);
            }
            OrderVo orderVo = new OrderVo();
            BeanUtils.copyProperties(order, orderVo);
            Map<String, Object> map = new HashMap<>();
            map.put("order", orderVo);
            map.put("items", itemVos);
            resultList.add(map);
        }
        log.info("用户{}订单共{}条", userId, resultList.size());
        return Result.success("订单查询成功", resultList);
    }

    @Override
    public Result orderQueryListByMerchant(Long merchantId) {
        // 先从 order_item 查出该商家的订单ID集合
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getMerchantId, merchantId);
        itemWrapper.select(OrderItem::getOrderId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        if (items.isEmpty()) {
            log.info("商家{}暂无订单", merchantId);
            return Result.success("订单查询成功", Collections.emptyList());
        }
        Set<Long> orderIds = items.stream()
                .map(OrderItem::getOrderId)
                .collect(Collectors.toSet());
        // 按 orderId 查订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getId, orderIds);
        wrapper.orderByDesc(Order::getCreateTime);
        List<Order> orders = orderMapper.selectList(wrapper);
        log.info("商家{}店铺的订单如下：{}", merchantId, orders);
        return Result.success("订单查询成功", orders);
    }


    @Override
    @Transactional
    public Result orderCancel(Long userId, String orderNo) {
        LambdaUpdateWrapper<Order> wrapper1 = new LambdaUpdateWrapper<>();
        wrapper1.eq(Order::getOrderNo, orderNo);
        wrapper1.eq(Order::getUserId, userId);
        wrapper1.set(Order::getStatus, ResultMsgConstant.STATUS_CANCELLED);
        wrapper1.set(Order::getUpdateTime, new Date());
        wrapper1.set(Order::getCancelTime, new Date());
        int update = orderMapper.update(wrapper1);
        if(update==0){
            log.error("用户{}的订单{}暂不存在，无法删除",userId,orderNo);
            return  Result.Fail("订单不存在，无法删除");
        }
        return Result.success("订单已删除");
    }

    @Override
    @Transactional
    public Result orderCreate(Long userId,OrderDto orderDto) {
        if(orderDto.getCar().isEmpty()){
            throw new BusinessException("没有购物车商品可以结算,请检查");
        }
        MqOrderMessage message = new MqOrderMessage();
        message.setUserId(userId);
        message.setOrderDto(orderDto);
        message.setCouponId(orderDto.getCouponId());
        message.setAddressSnapshot(orderDto.getAddressSnapshot());
        message.setMessageId(UUID.randomUUID().toString());
        mqSender.orderSend(MqConstant.MQ_ORDER_EXCHANGE,MqConstant.MQ_ORDER_ROUTING_KEY,message);
        return Result.success("下单成功，正在为您创建订单");
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo, String tradeNo) {
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getStatus, ResultMsgConstant.STATUS_PENDING_PAY);
        wrapper.set(Order::getStatus, ResultMsgConstant.STATUS_PAID);
        wrapper.set(Order::getPayType, "ALIPAY");
        wrapper.set(Order::getTradeNo, tradeNo);
        wrapper.set(Order::getPayTime, new Date());
        wrapper.set(Order::getUpdateTime, new Date());
        int rows = orderMapper.update(wrapper);
        if (rows == 0) {
            log.warn("订单{}支付回调更新失败：订单不存在或状态不是待支付", orderNo);
            throw new BusinessException("订单状态异常，支付回调处理失败");
        }
    }
}




