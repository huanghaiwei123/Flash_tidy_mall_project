package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户优惠券表
 * @TableName user_coupon
 */
@TableName(value = "user_coupon")
@Data
public class UserCoupon implements Serializable {
    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 状态：UNUSED=未使用 USED=已使用 EXPIRED=已过期
     */
    private String status;

    /**
     * 使用的订单ID
     */
    private Long orderId;

    /**
     * 使用时间
     */
    private Date usedTime;

    /**
     * 领取时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
