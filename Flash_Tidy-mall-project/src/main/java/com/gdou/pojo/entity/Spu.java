package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品 SPU 主表
 * @TableName spu
 */
@TableName(value ="spu")
@Data
public class Spu implements Serializable {
    /**
     * SPU ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 所属分类ID
     */
    private Long categoryId;

    /**
     * 所属分类名称
     */
    private String categoryName;

    /**
     * 商家id
     */
    private Long merchantId;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 主图 URL
     */
    private String mainImage;

    /**
     * 商品轮播图（JSON 数组）
     */
    private Object images;

    /**
     * 商品详情（富文本 HTML）
     */
    private String detail;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 状态：0=下架 1=上架
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}