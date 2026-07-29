package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/hhw/pay")
public class PayController {

    @Autowired
    private PayService payService;

    /**
     * mock支付
     */
    @PostMapping("/{orderId}")
    public Result pay(@PathVariable Long orderId) {
        log.info("支付订单 {}", orderId);
        return payService.pay(orderId);
    }

    /**
     * 取消支付，回补库存
     */
    @PostMapping("/cancel/{orderId}")
    public Result cancel(@PathVariable Long orderId) {
        log.info("取消订单 {}", orderId);
        return payService.cancel(orderId);
    }
}
