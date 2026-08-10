package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.common.Result;
import com.gdou.mapper.CouponMapper;
import com.gdou.mapper.CouponMerchantMapper;
import com.gdou.mapper.UserCouponMapper;
import com.gdou.pojo.dto.CouponDto;
import com.gdou.pojo.entity.Coupon;
import com.gdou.pojo.entity.CouponMerchant;
import com.gdou.pojo.entity.UserCoupon;
import com.gdou.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 优惠券 Service 实现
 */
@Service
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon>
        implements CouponService {

    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private CouponMerchantMapper couponMerchantMapper;
    @Autowired
    private UserCouponMapper userCouponMapper;

    // ===================== 管理员 =====================

    @Override
    @Transactional
    public Result createCoupon(CouponDto couponDto) {
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(couponDto, coupon);
        coupon.setIssuedCount(0);
        coupon.setCreateTime(new Date());
        coupon.setUpdateTime(new Date());
        save(coupon);
        log.info("管理员创建优惠券：{}", coupon.getName());
        return Result.success("优惠券创建成功", coupon);
    }

    @Override
    @Transactional
    public Result updateCoupon(Long couponId, CouponDto couponDto) {
        Coupon coupon = getById(couponId);
        if (coupon == null) {
            return Result.Fail("优惠券不存在");
        }
        BeanUtils.copyProperties(couponDto, coupon);
        coupon.setUpdateTime(new Date());
        updateById(coupon);
        log.info("管理员更新优惠券：{}", coupon.getName());
        return Result.success("优惠券更新成功", coupon);
    }

    @Override
    public Result adminQueryCoupons(Integer status) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreateTime);
        List<Coupon> list = list(wrapper);
        return Result.success(list);
    }

    // ===================== 商家 =====================

    @Override
    @Transactional
    public Result merchantOptIn(Long merchantId, Long couponId) {
        Coupon coupon = getById(couponId);
        if (coupon == null || coupon.getStatus() == 0) {
            return Result.Fail("优惠券不存在或已禁用");
        }
        // 检查是否已参与
        LambdaQueryWrapper<CouponMerchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponMerchant::getCouponId, couponId)
               .eq(CouponMerchant::getMerchantId, merchantId);
        CouponMerchant cm = couponMerchantMapper.selectOne(wrapper);
        if (cm != null && cm.getStatus() == 1) {
            return Result.Fail("已参与该优惠券，无需重复操作");
        }
        if (cm != null) {
            // 之前退出了，现在重新参与
            cm.setStatus(1);
            couponMerchantMapper.updateById(cm);
        } else {
            cm = new CouponMerchant();
            cm.setCouponId(couponId);
            cm.setMerchantId(merchantId);
            cm.setStatus(1);
            cm.setCreateTime(new Date());
            couponMerchantMapper.insert(cm);
        }
        log.info("商家 {} 参与优惠券 {}", merchantId, coupon.getName());
        return Result.success("已参与该优惠券");
    }

    @Override
    public Result merchantOptOut(Long merchantId, Long couponId) {
        LambdaQueryWrapper<CouponMerchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponMerchant::getCouponId, couponId)
               .eq(CouponMerchant::getMerchantId, merchantId);
        CouponMerchant cm = couponMerchantMapper.selectOne(wrapper);
        if (cm == null || cm.getStatus() == 0) {
            return Result.Fail("未参与该优惠券");
        }
        cm.setStatus(0);
        couponMerchantMapper.updateById(cm);
        log.info("商家 {} 退出优惠券 {}", merchantId, couponId);
        return Result.success("已退出该优惠券");
    }

    @Override
    public Result merchantQueryCoupons(Long merchantId) {
        // 查商家参与的所有优惠券ID
        LambdaQueryWrapper<CouponMerchant> cmWrapper = new LambdaQueryWrapper<>();
        cmWrapper.eq(CouponMerchant::getMerchantId, merchantId)
                 .eq(CouponMerchant::getStatus, 1);
        List<CouponMerchant> cmList = couponMerchantMapper.selectList(cmWrapper);

        if (cmList.isEmpty()) {
            return Result.success("暂未参与任何优惠券", null);
        }
        // 查出对应优惠券
        List<Long> couponIds = cmList.stream()
                .map(CouponMerchant::getCouponId)
                .collect(Collectors.toList());
        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);

        return Result.success(coupons);
    }

    // ===================== 用户 =====================

    @Override
    public Result userQueryAvailableCoupons() {
        // 查所有有效且在有效期内的优惠券
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, 1)
               .le(Coupon::getStartTime, new Date())
               .ge(Coupon::getEndTime, new Date())
               .apply("issued_count < total_count")  // 还没领完
               .orderByDesc(Coupon::getCreateTime);
        List<Coupon> list = list(wrapper);

        // 过滤：只有已被至少一个商家参与的优惠券才展示
        List<Coupon> available = list.stream().filter(coupon -> {
            LambdaQueryWrapper<CouponMerchant> cmWrapper = new LambdaQueryWrapper<>();
            cmWrapper.eq(CouponMerchant::getCouponId, coupon.getId())
                     .eq(CouponMerchant::getStatus, 1);
            return couponMerchantMapper.selectCount(cmWrapper) > 0;
        }).collect(Collectors.toList());

        return Result.success(available);
    }

    @Override
    @Transactional
    public Result collectCoupon(Long userId, Long couponId) {
        Coupon coupon = getById(couponId);
        if (coupon == null || coupon.getStatus() == 0) {
            return Result.Fail("优惠券不存在或已禁用");
        }
        Date now = new Date();
        if (now.before(coupon.getStartTime()) || now.after(coupon.getEndTime())) {
            return Result.Fail("不在优惠券有效期内");
        }
        if (coupon.getIssuedCount() >= coupon.getTotalCount()) {
            return Result.Fail("优惠券已被领完");
        }
        // 检查每人限领
        LambdaQueryWrapper<UserCoupon> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.eq(UserCoupon::getUserId, userId)
                 .eq(UserCoupon::getCouponId, couponId);
        int userCollected = userCouponMapper.selectCount(ucWrapper).intValue();
        if (userCollected >= coupon.getPerUserLimit()) {
            return Result.Fail("每人限领" + coupon.getPerUserLimit() + "张，您已达到上限");
        }
        // 发券
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus("UNUSED");
        userCoupon.setCreateTime(now);
        userCouponMapper.insert(userCoupon);

        // 已发放数+1
        LambdaUpdateWrapper<Coupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Coupon::getId, couponId)
                     .setSql("issued_count = issued_count + 1");
        update(null, updateWrapper);

        log.info("用户 {} 领取优惠券 {}", userId, coupon.getName());
        return Result.success("领取成功", userCoupon);
    }

    @Override
    public Result queryMyCoupons(Long userId, String status) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(UserCoupon::getStatus, status);
        }
        wrapper.orderByDesc(UserCoupon::getCreateTime);
        List<UserCoupon> list = userCouponMapper.selectList(wrapper);

        if (list.isEmpty()) {
            return Result.success(list);
        }

        // 批量查优惠券详情
        Set<Long> couponIds = list.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toSet());
        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
        Map<Long, Coupon> couponMap = new HashMap<>();
        for (Coupon c : coupons) {
            couponMap.put(c.getId(), c);
        }

        // 合并返回：UserCoupon 状态 + Coupon 详情
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCoupon uc : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", uc.getId());
            item.put("status", uc.getStatus());
            item.put("usedTime", uc.getUsedTime());
            item.put("createTime", uc.getCreateTime());
            Coupon c = couponMap.get(uc.getCouponId());
            if (c != null) {
                item.put("couponName", c.getName());
                item.put("type", c.getType());
                item.put("discountValue", c.getDiscountValue());
                item.put("minAmount", c.getMinAmount());
                item.put("startTime", c.getStartTime());
                item.put("endTime", c.getEndTime());
            }
            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result validateCoupon(Long userId, Long userCouponId, BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            return Result.Fail("优惠券不存在");
        }
        if (!userCoupon.getUserId().equals(userId)) {
            return Result.Fail("该优惠券不属于当前用户");
        }
        if (!"UNUSED".equals(userCoupon.getStatus())) {
            return Result.Fail("优惠券无法使用（已使用或已过期）");
        }
        Coupon coupon = getById(userCoupon.getCouponId());
        if (coupon == null || coupon.getStatus() == 0) {
            return Result.Fail("优惠券已失效");
        }
        Date now = new Date();
        if (now.before(coupon.getStartTime()) || now.after(coupon.getEndTime())) {
            // 标记为过期
            userCoupon.setStatus("EXPIRED");
            userCouponMapper.updateById(userCoupon);
            return Result.Fail("优惠券已过期");
        }
        // 检查门槛金额
        if (orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            return Result.Fail("未达到使用门槛，需满" + coupon.getMinAmount() + "元");
        }
        // 计算优惠金额
        BigDecimal discountAmount;
        if ("FULL_REDUCTION".equals(coupon.getType())) {
            discountAmount = coupon.getDiscountValue();
        } else if ("DISCOUNT".equals(coupon.getType())) {
            discountAmount = orderAmount.multiply(
                    BigDecimal.ONE.subtract(coupon.getDiscountValue()));
        } else {
            return Result.Fail("未知的优惠券类型");
        }
        log.info("用户 {} 验证优惠券 {} 通过，优惠金额：{}", userId, userCouponId, discountAmount);
        return Result.success("验证通过", discountAmount);
    }
}
