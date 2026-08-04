package com.gdou.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.gdou.config.AlipayProperties;
import com.gdou.exception.BusinessException;
import com.gdou.pojo.entity.SeckillOrder;
import com.gdou.service.AlipayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class AlipayServiceImpl implements AlipayService {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private AlipayProperties props;

    @Override
    public String generatePayPage(SeckillOrder seckillOrder) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
//        异步回调地址
        request.setNotifyUrl(props.getNotifyUrl());
//        支付完成跳转地址
        request.setReturnUrl(props.getReturnUrl());
        // 业务参数
        request.setBizContent(
                "{" +
                        "\"out_trade_no\":\"" + seckillOrder.getOrderId() + "\"," +
                        "\"total_amount\":\"" + seckillOrder.getPrice().toString() + "\"," +
                        "\"subject\":\"" + seckillOrder.getGoodsName() + "\"," +
                        "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"" +
                        "}"
        );
        try {
            // pageExecute 返回 Form 表单形式的 HTML，浏览器直接渲染即可跳转支付宝
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("生成支付宝支付页面失败，订单号: {}", seckillOrder.getOrderId(), e);
            throw new BusinessException("创建支付请求失败，请稍后重试");
        }
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    props.getAlipayPublicKey(),
                    props.getCharset(),
                    props.getSignType()
            );
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签失败", e);
            return false;
        }

    }

    @Override
    public String executeRefund(SeckillOrder seckillOrder) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizContent(
                "{" +
                        "\"out_trade_no\":\"" + seckillOrder.getOrderId() + "\"," +
                        "\"refund_amount\":\"" +seckillOrder.getPrice().toString() + "\"" +
                         "}"
        );

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("退款成功，订单号: {}, 支付宝退款流水: {}",
                        seckillOrder.getOrderId(), response.getTradeNo());
                return response.getTradeNo();
            } else {
                log.error("退款失败，订单号: {}, 错误: {}-{}",
                        seckillOrder.getOrderId(), response.getCode(), response.getSubMsg());
                throw new BusinessException("退款失败: " + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("调用支付宝退款接口异常，订单号: {}", seckillOrder.getOrderId(), e);
            throw new BusinessException("退款请求异常，请稍后重试");
        }
    }
}
