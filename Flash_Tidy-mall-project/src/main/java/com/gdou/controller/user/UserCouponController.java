package com.gdou.controller.user;

import com.gdou.common.Result;
import com.gdou.service.CouponService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 用户 — 优惠券领取 & 查询
 */
@RestController
@RequestMapping("/hhw/user/coupon")
@Slf4j
public class UserCouponController {

    @Autowired
    private CouponService couponService;

    /**
     * 用户查看可领取的优惠券列表
     */
    @GetMapping("/available")
    public Result available() {
        return couponService.userQueryAvailableCoupons();
    }

    /**
     * 用户领取优惠券
     */
    @PostMapping("/collect/{couponId}")
    public Result collect(@PathVariable Long couponId) {
        Long userId = UserHolder.get();
        log.info("用户 {} 领取优惠券 {}", userId, couponId);
        return couponService.collectCoupon(userId, couponId);
    }

    /**
     * 用户查看自己的优惠券（可筛选状态：UNUSED / USED / EXPIRED）
     */
    @GetMapping("/mine")
    public Result mine(@RequestParam(required = false) String status) {
        Long userId = UserHolder.get();
        return couponService.queryMyCoupons(userId, status);
    }

    /**
     * 下单时验证优惠券是否可用
     */
    @PostMapping("/validate/{userCouponId}")
    public Result validate(@PathVariable Long userCouponId,
                           @RequestParam BigDecimal orderAmount) {
        Long userId = UserHolder.get();
        return couponService.validateCoupon(userId, userCouponId, orderAmount);
    }
}
