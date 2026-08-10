package com.gdou.controller.admin;

import com.gdou.common.Result;
import com.gdou.pojo.dto.CouponDto;
import com.gdou.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 管理员 — 优惠券管理
 */
@RestController
@RequestMapping("/hhw/admin/coupon")
@Slf4j
public class CouponController {

    @Autowired
    private CouponService couponService;

    /**
     * 创建优惠券
     */
    @PostMapping
    public Result create(@Valid @RequestBody CouponDto couponDto) {
        log.info("管理员创建优惠券");
        return couponService.createCoupon(couponDto);
    }

    /**
     * 更新优惠券
     */
    @PutMapping("/{couponId}")
    public Result update(@PathVariable Long couponId, @Valid @RequestBody CouponDto couponDto) {
        log.info("管理员更新优惠券：{}", couponId);
        return couponService.updateCoupon(couponId, couponDto);
    }

    /**
     * 查询优惠券列表（可按状态筛选）
     */
    @GetMapping
    public Result list(@RequestParam(required = false) Integer status) {
        return couponService.adminQueryCoupons(status);
    }
}
