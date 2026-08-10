package com.gdou.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gdou.common.Result;
import com.gdou.config.AlipayProperties;
import com.gdou.constant.ResultCodeConstant;
import com.gdou.constant.ResultMsgConstant;
import com.gdou.exception.BusinessException;
import com.gdou.mapper.OrderMapper;
import com.gdou.pojo.entity.Order;
import com.gdou.service.OrderService;
import com.gdou.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 支付服务实现 — 支付宝沙箱页面支付
 */
@Service
@Slf4j
public class PayServiceImpl implements PayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayProperties alipayProperties;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result pay(Long userId, String orderNo) {
        // 1. 查订单 + 校验归属
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!ResultMsgConstant.STATUS_PENDING_PAY.equals(order.getStatus())) {
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 2. 构建支付宝页面支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        request.setReturnUrl(alipayProperties.getReturnUrl());

        String bizContent = "{" +
                "\"out_trade_no\":\"" + orderNo + "\"," +
                "\"total_amount\":" + order.getPayAmount().toString() + "," +
                "\"subject\":\"潮汐商城订单" + orderNo + "\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"" +
                "}";
        request.setBizContent(bizContent);

        // 3. 调用支付宝获取支付表单 HTML
        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                log.info("订单{}支付请求已生成", orderNo);
                return Result.success("支付发起成功", response.getBody());
            } else {
                log.error("支付宝支付发起失败: code={}, msg={}", response.getCode(), response.getMsg());
                throw new BusinessException("支付发起失败: " + response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝API调用异常", e);
            throw new BusinessException("支付系统异常，请稍后重试");
        }
    }

    @Override
    public String handleNotify(Map<String, String> params) {
        log.info("收到支付宝异步通知: out_trade_no={}, trade_status={}",
                params.get("out_trade_no"), params.get("trade_status"));

        try {
            // 1. RSA2 验签
            String pubKey = alipayProperties.getAlipayPublicKey();
            log.info("当前支付宝公钥长度={}, 前30位={}", pubKey != null ? pubKey.length() : 0,
                    pubKey != null ? pubKey.substring(0, Math.min(30, pubKey.length())) : "null");
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    pubKey,
                    "UTF-8",
                    "RSA2"
            );

            if (!signVerified) {
                log.error("支付宝异步通知验签失败");
                return "failure";
            }

            // 2. 检查交易状态
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                log.warn("支付宝交易状态非成功: {}", tradeStatus);
                return "failure";
            }

            // 3. 校验 app_id（防止伪造通知）
            String appId = params.get("app_id");
            if (!alipayProperties.getAppId().equals(appId)) {
                log.error("支付宝通知app_id不匹配: {}", appId);
                return "failure";
            }

            // 4. 提取参数
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmount = params.get("total_amount");

            // 5. 校验订单金额（防止金额篡改）
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getOrderNo, outTradeNo);
            Order order = orderMapper.selectOne(wrapper);
            if (order == null) {
                log.error("支付回调订单不存在: {}", outTradeNo);
                return "failure";
            }
            if (order.getPayAmount().compareTo(new BigDecimal(totalAmount)) != 0) {
                log.error("支付金额不匹配: 订单{}元, 支付宝{}元", order.getPayAmount(), totalAmount);
                return "failure";
            }

            // 6. 更新订单状态为已支付
            orderService.paySuccess(outTradeNo, tradeNo);
            log.info("订单{}支付成功，支付宝交易号: {}", outTradeNo, tradeNo);
            return "success";

        } catch (AlipayApiException e) {
            log.error("支付宝验签异常", e);
            return "failure";
        } catch (BusinessException e) {
            log.warn("支付回调业务异常: {}", e.getMessage());
            return "failure";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result refund(Long userId, String orderNo) {
        // 1. 查订单 + 校验归属
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getUserId, userId);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!ResultMsgConstant.STATUS_PAID.equals(order.getStatus())) {
            throw new BusinessException("订单状态不支持退款");
        }
        if (order.getTradeNo() == null || order.getTradeNo().isEmpty()) {
            throw new BusinessException("订单无支付宝交易号，无法退款");
        }

        // 2. 先调支付宝退款 API（成功后再改本地状态，避免 REFUNDING 卡住）
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        String refundNo = orderNo + "_REFUND_" + System.currentTimeMillis();
        String bizContent = "{" +
                "\"out_trade_no\":\"" + orderNo + "\"," +
                "\"refund_amount\":" + order.getPayAmount().toString() + "," +
                "\"out_request_no\":\"" + refundNo + "\"" +
                "}";
        request.setBizContent(bizContent);

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.error("支付宝退款失败: code={}, msg={}", response.getCode(), response.getMsg());
                throw new BusinessException("退款失败: " + response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("退款API调用异常", e);
            throw new BusinessException("退款系统异常，请稍后重试");
        }

        // 3. 支付宝退款成功，乐观锁更新状态 + 恢复库存
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getOrderNo, orderNo);
        updateWrapper.eq(Order::getStatus, ResultMsgConstant.STATUS_PAID);
        updateWrapper.set(Order::getStatus, ResultMsgConstant.STATUS_REFUNDED);
        updateWrapper.set(Order::getUpdateTime, new Date());
        int rows = orderMapper.update(updateWrapper);
        if (rows == 0) {
            // 极端情况：支付宝已退款但本地状态已变更，需人工核查
            log.error("订单{}支付宝已退款但本地状态更新失败（状态非PAID），需人工处理", orderNo);
            throw new BusinessException("退款状态异常，请联系客服处理");
        }

        orderService.restoreStock(orderNo);
        log.info("订单{}退款成功，库存已恢复", orderNo);
        return Result.success("退款成功");
    }

    @Override
    public void queryPayResult(String orderNo) {
        // 只查待支付状态的订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        wrapper.eq(Order::getStatus, ResultMsgConstant.STATUS_PENDING_PAY);
        Order order = orderMapper.selectOne(wrapper);
        if (order == null) {
            log.info("订单{}无需查询（不存在或非待支付）", orderNo);
            return;
        }
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess() && "TRADE_SUCCESS".equals(response.getTradeStatus())) {
                log.info("查询到订单{}已支付，支付宝交易号: {}", orderNo, response.getTradeNo());
                orderService.paySuccess(orderNo, response.getTradeNo());
            } else {
                log.info("查询订单{}结果为: code={}, status={}", orderNo,
                        response.getCode(), response.getTradeStatus());
            }
        } catch (Exception e) {
            log.error("查询支付宝支付结果异常: {}", e.getMessage());
        }
    }
}
