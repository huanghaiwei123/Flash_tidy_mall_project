package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/hhw/pay")
public class PayController {

    @Autowired
    private PayService payService;

    /**
     * 支付宝支付返回支付成功页面
     */
    @PostMapping("/{orderId}")
    public void pay(@PathVariable Long orderId, HttpServletResponse response) throws IOException {
        log.info("支付订单 {}", orderId);
        String html = payService.pay(orderId);
        response.setContentType("text/html;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.write(html);
        writer.flush();
    }

    /**
     * 取消支付，回补库存
     */
    @PostMapping("/cancel/{orderId}")
    public Result cancel(@PathVariable Long orderId) {
        log.info("取消订单 {}", orderId);
        return payService.cancel(orderId);
    }

    /**
     * 退款
     * @param orderId
     * @return
     */
    @PostMapping("/refund/{orderId}")
    public Result refund(@PathVariable Long orderId) {
        log.info("退款，订单号: {}", orderId);
        return payService.refund(orderId);
    }

    /**
     * 支付宝异步回调（服务器到服务器，不需要 JWT）
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        log.info("收到支付宝异步回调，订单号: {}", params.get("out_trade_no"));
        return payService.handleAlipayNotify(params);
    }
    /**
     * 支付完成同步跳转（浏览器重定向到此，不需要 JWT）
     */
    @GetMapping("/return")
    public void syncReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String outTradeNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");
        log.info("支付同步回调，订单号: {}, 交易号: {}", outTradeNo, tradeNo);
        // 重定向到前端结果页，把参数带过去
        response.sendRedirect("http://localhost:8080/hhw/seckill/seckillResult/" + outTradeNo);
    }

}
