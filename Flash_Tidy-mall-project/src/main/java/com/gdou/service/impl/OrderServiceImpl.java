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
import com.gdou.service.ShoppingCarService;
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
    @Autowired
    private ShoppingCarService shoppingCarService;
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
        wrapper1.eq(Order::getStatus, ResultMsgConstant.STATUS_PENDING_PAY);
        wrapper1.set(Order::getStatus, ResultMsgConstant.STATUS_CANCELLED);
        wrapper1.set(Order::getUpdateTime, new Date());
        wrapper1.set(Order::getCancelTime, new Date());
        int update = orderMapper.update(wrapper1);
        if(update==0){
            log.error("用户{}的订单{}不存在或状态非待支付，无法取消", userId, orderNo);
            return Result.Fail("订单不存在或状态异常，无法取消");
        }
        // 恢复锁定库存
        restoreStock(orderNo);
        return Result.success("订单已取消");
    }

    @Override
    @Transactional
    public Result orderCreate(Long userId,OrderDto orderDto,String orderType) {
        if(orderDto.getCar().isEmpty()){
            throw new BusinessException("没有购物车商品可以结算,请检查");
        }
        MqOrderMessage message = new MqOrderMessage();
        message.setUserId(userId);
        message.setOrderDto(orderDto);
        message.setMessageId(UUID.randomUUID().toString());
        message.setOrderType(orderType);
        mqSender.orderSend(MqConstant.MQ_ORDER_EXCHANGE,MqConstant.MQ_ORDER_ROUTING_KEY,message);
        return Result.success("下单成功，正在为您创建订单");
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo, String tradeNo) {
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(wrapper);
        if (order == null) {
            log.error("支付回调订单不存在: {}", orderNo);
            throw new BusinessException("订单不存在");
        }
        if (!order.getStatus().equals(ResultMsgConstant.STATUS_PENDING_PAY)) {
            log.warn("订单{}状态异常，当前状态: {}, 非待支付", orderNo, order.getStatus());
            throw new BusinessException("订单状态异常");
        }
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
//        订单支付成功后扣减可售库存和锁住库存
        LambdaQueryWrapper<OrderItem> orderItemLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderItemLambdaQueryWrapper.eq(OrderItem::getOrderNo, orderNo);
        List<OrderItem> orderItems = orderItemMapper.selectList(orderItemLambdaQueryWrapper);
        for (OrderItem orderItem : orderItems) {
            Long skuId = orderItem.getSkuId();
            LambdaUpdateWrapper<Sku> skuLambdaUpdateWrapper = new LambdaUpdateWrapper<Sku>().eq(Sku::getId, skuId);
            Integer quantity = orderItem.getQuantity();
            if(order.getOrderType().equals("SECKILL")){
                skuLambdaUpdateWrapper.setSql("locked_stock = locked_stock - ?," +
                        "stock = stock - ?," +
                        "promotion_stock = promotion_stock - ?",quantity,quantity,quantity);
            }else{
                skuLambdaUpdateWrapper.setSql("locked_stock = locked_stock - ?,stock = stock - ?",quantity,quantity);
            }
            int update = skuMapper.update(skuLambdaUpdateWrapper);
            if (update == 0) {
                throw new BusinessException("订单支付成功后商品库存更新失败");
            }else{
                log.info("订单支付成功后商品库存更新成功");
            }
        }
    }

    @Override
    @Transactional
    public Result ship(Long merchantId, String orderNo) {
        // 1. 查该商家在此订单的明细，只发 PENDING 状态的
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderNo, orderNo);
        itemWrapper.eq(OrderItem::getMerchantId, merchantId);
        itemWrapper.eq(OrderItem::getItemStatus, ResultMsgConstant.ITEM_STATUS_PENDING);
        Long itemCount = orderItemMapper.selectCount(itemWrapper);
        if (itemCount == 0) {
            throw new BusinessException("没有可发货的订单明细（可能已发货或不属于该商家）");
        }
        // 2. 更新该商家明细为已发货
        LambdaUpdateWrapper<OrderItem> itemUpdate = new LambdaUpdateWrapper<>();
        itemUpdate.eq(OrderItem::getOrderNo, orderNo);
        itemUpdate.eq(OrderItem::getMerchantId, merchantId);
        itemUpdate.eq(OrderItem::getItemStatus, ResultMsgConstant.ITEM_STATUS_PENDING);
        itemUpdate.set(OrderItem::getItemStatus, ResultMsgConstant.ITEM_STATUS_SHIPPED);
        orderItemMapper.update(itemUpdate);
        // 3. 若订单状态是 PAID，更新为 SHIPPED
        LambdaUpdateWrapper<Order> orderWrapper = new LambdaUpdateWrapper<>();
        orderWrapper.eq(Order::getOrderNo, orderNo);
        orderWrapper.eq(Order::getStatus, ResultMsgConstant.STATUS_PAID);
        orderWrapper.set(Order::getStatus, ResultMsgConstant.STATUS_SHIPPED);
        orderWrapper.set(Order::getUpdateTime, new Date());
        orderMapper.update(orderWrapper);
        log.info("商家{}对订单{}的{}件商品发货成功", merchantId, orderNo, itemCount);
        return Result.success("发货成功，" + itemCount + " 件商品已发货");
    }

    @Override
    @Transactional
    public Result receive(Long userId, String orderNo) {
        // 1. 校验订单归属
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!ResultMsgConstant.STATUS_SHIPPED.equals(order.getStatus())
                && !ResultMsgConstant.STATUS_PAID.equals(order.getStatus())) {
            throw new BusinessException("订单状态异常，只能确认已付款或已发货的订单");
        }
        // 2. 将该订单所有明细标记为已收货
        LambdaUpdateWrapper<OrderItem> itemUpdate = new LambdaUpdateWrapper<>();
        itemUpdate.eq(OrderItem::getOrderNo, orderNo);
        itemUpdate.set(OrderItem::getItemStatus, ResultMsgConstant.ITEM_STATUS_RECEIVED);
        orderItemMapper.update(itemUpdate);
        // 3. 订单状态 → 已完成
        LambdaUpdateWrapper<Order> orderUpdate = new LambdaUpdateWrapper<>();
        orderUpdate.eq(Order::getOrderNo, orderNo);
        orderUpdate.set(Order::getStatus, ResultMsgConstant.STATUS_COMPLETED);
        orderUpdate.set(Order::getUpdateTime, new Date());
        orderMapper.update(orderUpdate);
        log.info("用户{}确认收货订单{}，订单已完成", userId, orderNo);
        return Result.success("确认收货成功，订单已完成");
    }

    @Override
    @Transactional
    public void restoreStock(String orderNo) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderNo, orderNo);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        for (OrderItem item : items) {
            LambdaUpdateWrapper<Sku> skuWrapper = new LambdaUpdateWrapper<>();
            skuWrapper.eq(Sku::getId, item.getSkuId());
            skuWrapper.eq(Sku::getSpuId, item.getSpuId());
            skuWrapper.ge(Sku::getLockedStock, item.getQuantity());
            skuWrapper.setSql("available_stock = available_stock + " + item.getQuantity()
                    + ", locked_stock = locked_stock - " + item.getQuantity());
            skuMapper.update(null, skuWrapper);
        }
        log.info("订单{}库存已恢复，共{}件商品", orderNo, items.size());
    }

    @Override
    public Result reorder(Long userId, String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderNo, orderNo);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        int count = 0;
        for (OrderItem item : items) {
            shoppingCarService.addToCart(userId, item.getSpuId(), item.getSkuId(), item.getQuantity());
            count++;
        }
        log.info("用户{}对订单{}再来一单，共{}件商品加入购物车", userId, orderNo, count);
        return Result.success("已将 " + count + " 件商品加入购物车");
    }
}




