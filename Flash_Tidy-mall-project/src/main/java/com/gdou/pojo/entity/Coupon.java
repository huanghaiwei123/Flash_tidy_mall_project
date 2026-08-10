package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券模板表（管理员创建）
 * @TableName coupon
 */
@TableName(value = "coupon")
@Data
public class Coupon implements Serializable {
    /**
     * 优惠券ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 类型：FULL_REDUCTION=满减 DISCOUNT=折扣
     */
    private String type;

    /**
     * 优惠值（满减=减多少元；折扣=0.85即85折）
     */
    private BigDecimal discountValue;

    /**
     * 最低消费金额（0=无门槛）
     */
    private BigDecimal minAmount;

    /**
     * 发放总量
     */
    private Integer totalCount;

    /**
     * 已领取数量
     */
    private Integer issuedCount;

    /**
     * 每人限领数量
     */
    private Integer perUserLimit;

    /**
     * 有效期开始
     */
    private Date startTime;

    /**
     * 有效期结束
     */
    private Date endTime;

    /**
     * 状态：0=禁用 1=正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
