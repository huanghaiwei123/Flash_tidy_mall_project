package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.service.OrderService;
import com.gdou.service.PayService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器 — 支付宝沙箱支付
 */
@RestController
@RequestMapping("/hhw/pay")
@Slf4j
public class PayController {

    @Autowired
    private PayService payService;

    @Autowired
    private OrderService orderService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * 发起支付 — 返回支付宝页面支付表单 HTML
     */
    @PostMapping("/{orderNo}")
    public Result pay(@PathVariable String orderNo) {
        Long userId = UserHolder.get();
        log.info("用户{}发起支付，订单号: {}", userId, orderNo);
        return payService.pay(userId, orderNo);
    }

    /**
     * 支付宝异步通知回调 — 无需登录认证（已在 WebConfig 排除拦截）
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(entry.getKey(), valueStr);
        }
        log.info("支付宝异步通知: out_trade_no={}, trade_status={}, sign={}",
                params.get("out_trade_no"), params.get("trade_status"),
                params.get("sign") != null ? params.get("sign").substring(0, 20) + "..." : "null");
        return payService.handleNotify(params);
    }

    /**
     * 支付宝同步回调 — 支付成功后 302 跳转回本站
     * 主动查询支付结果作为异步通知的备用方案
     */
    @GetMapping("/return")
    public void payReturn(HttpServletRequest request, javax.servlet.http.HttpServletResponse response) {
        String outTradeNo = request.getParameter("out_trade_no");
        log.info("支付宝同步回调到达, out_trade_no={}", outTradeNo);
        // 主动查询支付宝确认支付状态
        if (outTradeNo != null && !outTradeNo.isEmpty()) {
            payService.queryPayResult(outTradeNo);
        }
        try {
            response.sendRedirect(frontendUrl + "/order-list.html");
        } catch (java.io.IOException e) {
            log.error("支付同步回调重定向失败", e);
        }
    }

    /**
     * 退款
     */
    @PostMapping("/refund/{orderNo}")
    public Result refund(@PathVariable String orderNo) {
        Long userId = UserHolder.get();
        log.info("用户{}申请退款，订单号: {}", userId, orderNo);
        return payService.refund(userId, orderNo);
    }
}
