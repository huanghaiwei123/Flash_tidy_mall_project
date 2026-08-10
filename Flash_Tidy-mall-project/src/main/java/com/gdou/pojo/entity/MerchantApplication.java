package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商家申请表
 * @TableName merchant_application
 */
@TableName(value = "merchant_application")
@Data
public class MerchantApplication implements Serializable {
    /**
     * 申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请人用户ID
     */
    private Long userId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺简介
     */
    private String shopDescription;

    /**
     * 申请状态：PENDING=待审核 APPROVED=已通过 REJECTED=已拒绝
     */
    private String status;

    /**
     * 审核备注
     */
    private String reviewComment;

    /**
     * 审核人ID（管理员）
     */
    private Long reviewerId;

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
