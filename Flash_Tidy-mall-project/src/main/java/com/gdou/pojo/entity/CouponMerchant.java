package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 优惠券-商家关联表
 * @TableName coupon_merchant
 */
@TableName(value = "coupon_merchant")
@Data
public class CouponMerchant implements Serializable {
    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 商家用户ID
     */
    private Long merchantId;

    /**
     * 是否参与：0=不参与 1=参与
     */
    private Integer status;

    /**
     * 参与时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
