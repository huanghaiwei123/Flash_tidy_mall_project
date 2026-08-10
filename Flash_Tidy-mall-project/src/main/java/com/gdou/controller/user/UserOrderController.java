package com.gdou.controller.user;

import com.gdou.common.Result;
import com.gdou.pojo.dto.OrderDto;
import com.gdou.pojo.entity.Order;
import com.gdou.service.OrderService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hhw/user")
@Slf4j
public class UserOrderController {
    @Autowired
    private OrderService orderService;
    /**
     * 单个订单查询
     * @return
     */
    @GetMapping("/order/query")
    public Result orderQuery(@RequestParam String orderNo) {
        Long userId = UserHolder.get();
        return orderService.orderQueryByUser(userId,orderNo);
    }


    /**
     * 批量订单查询
     * @return
     */
    @GetMapping("/order/queryList")
    public Result orderQueryList() {
        Long userId = UserHolder.get();
        log.info("用户{}正在查询所有订单",userId);
        return orderService.orderQueryListByUser(userId);
    }

    /**
     * 用户普通下单
     * @param orderDto
     * @return
     */
    @PostMapping("/order/create")
    public Result orderCreate(@RequestBody OrderDto orderDto) {
        Long userId = UserHolder.get();
        log.info("用户{}正在在创建订单",userId);
        return orderService.orderCreate(userId,orderDto);
    }

    /**
     * 用户取消订单
     * @param orderNo
     * @return
     */
    @PostMapping("/order/delete/{orderNo}")
    public Result orderCancel(@PathVariable String orderNo) {
        log.info("用户{}正在取消标号为{}的订单",UserHolder.get(),orderNo);
        return orderService.orderCancel(UserHolder.get(),orderNo);
    }


}
