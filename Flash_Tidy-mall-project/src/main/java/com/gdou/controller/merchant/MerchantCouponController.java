package com.gdou.controller.merchant;

import com.gdou.common.Result;
import com.gdou.service.CouponService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 商家 — 优惠券参与管理
 */
@RestController
@RequestMapping("/hhw/merchant/coupon")
@Slf4j
public class MerchantCouponController {

    @Autowired
    private CouponService couponService;

    /**
     * 商家查看所有可选优惠券（管理员已创建的）
     */
    @GetMapping("/available")
    public Result available() {
        return couponService.adminQueryCoupons(1);
    }

    /**
     * 商家参与优惠券
     */
    @PostMapping("/opt-in/{couponId}")
    public Result optIn(@PathVariable Long couponId) {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 参与优惠券 {}", merchantId, couponId);
        return couponService.merchantOptIn(merchantId, couponId);
    }

    /**
     * 商家退出优惠券
     */
    @PostMapping("/opt-out/{couponId}")
    public Result optOut(@PathVariable Long couponId) {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 退出优惠券 {}", merchantId, couponId);
        return couponService.merchantOptOut(merchantId, couponId);
    }

    /**
     * 商家查看已参与的优惠券
     */
    @GetMapping("/mine")
    public Result mine() {
        Long merchantId = UserHolder.get();
        return couponService.merchantQueryCoupons(merchantId);
    }
}
