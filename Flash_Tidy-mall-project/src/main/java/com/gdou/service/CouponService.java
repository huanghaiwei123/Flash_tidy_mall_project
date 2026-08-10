package com.gdou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdou.common.Result;
import com.gdou.pojo.dto.CouponDto;
import com.gdou.pojo.entity.Coupon;

/**
 * 优惠券 Service
 */
public interface CouponService extends IService<Coupon> {

    /**
     * 管理员创建优惠券
     */
    Result createCoupon(CouponDto couponDto);

    /**
     * 管理员更新优惠券
     */
    Result updateCoupon(Long couponId, CouponDto couponDto);

    /**
     * 管理员查询所有优惠券
     */
    Result adminQueryCoupons(Integer status);

    /**
     * 商家选择参与优惠券
     */
    Result merchantOptIn(Long merchantId, Long couponId);

    /**
     * 商家取消参与优惠券
     */
    Result merchantOptOut(Long merchantId, Long couponId);

    /**
     * 商家查询自己参与的优惠券
     */
    Result merchantQueryCoupons(Long merchantId);

    /**
     * 用户查询可领取的优惠券（只展示有商家参与的，含是否已领取标记）
     */
    Result userQueryAvailableCoupons(Long userId);

    /**
     * 用户领取优惠券
     */
    Result collectCoupon(Long userId, Long couponId);

    /**
     * 用户查询自己的优惠券
     */
    Result queryMyCoupons(Long userId, String status);

    /**
     * 验证优惠券是否可用（下单时校验）
     */
    Result validateCoupon(Long userId, Long userCouponId, java.math.BigDecimal orderAmount);
}
