package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商家表（店铺信息）
 * id 与 user.id 一致——商家即用户
 * @TableName merchant
 */
@TableName(value = "merchant")
@Data
public class Merchant implements Serializable {
    /**
     * 商家ID（= 用户ID，与 user.id 一致，不自增）
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺 Logo URL
     */
    private String shopLogo;

    /**
     * 店铺简介
     */
    private String shopDescription;

    /**
     * 店铺联系电话
     */
    private String contactPhone;

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
