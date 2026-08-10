package com.gdou.controller.merchant;

import com.gdou.common.Result;
import com.gdou.service.OrderService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hhw/merchant")
@Slf4j
public class MerchantOrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 商家根据用户的订单编号查询订单详情
     * @param orderNo
     * @return
     */
    @GetMapping("/order/query")
    public Result queryByOrderNo(@RequestParam String orderNo) {
        Long merchantId = UserHolder.get();
        log.info("商家{}正在根据查询订单编号为{}的订单",merchantId,orderNo);
        return orderService.orderQueryByMerchant(merchantId,orderNo);
    }

    /**
     * 商家自己店铺的订单详情
     * @return
     */
    @GetMapping("/order/queryList")
    public Result orderQueryList() {
        Long userId = UserHolder.get();
        log.info("商家{}正在查询所有订单",userId);
        return orderService.orderQueryListByMerchant(userId);
    }

    /**
     * 商家发货
     * @param orderNo 订单编号
     * @return
     */
    @PostMapping("/order/ship/{orderNo}")
    public Result orderShip(@PathVariable String orderNo) {
        Long merchantId = UserHolder.get();
        log.info("商家{}正在对订单{}进行发货", merchantId, orderNo);
        return orderService.ship(merchantId, orderNo);
    }
}
