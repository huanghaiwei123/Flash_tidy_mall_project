package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 商品评论表（楼中楼）
 * @TableName product_comment
 */
@TableName(value = "product_comment")
@Data
public class ProductComment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(exist = false)
    private List<ProductComment> replies;

    /** 商品ID */
    private Long spuId;

    /** 用户昵称（非表字段，查询时组装） */
    @TableField(exist = false)
    private String userName;

    /** 被回复人昵称（非表字段，查询时组装） */
    @TableField(exist = false)
    private String replyUserName;

    /** 商品名称（非表字段，商家查看时组装） */
    @TableField(exist = false)
    private String spuName;

    /** SKU ID（可选，买过可评具体规格） */
    private Long skuId;

    /** 关联订单号（可选，买过才传） */
    private String orderNo;

    /** 发表者ID（用户或商家） */
    private Long userId;

    /** 父评论ID，null=顶级评论 */
    private Long parentId;

    /** 被回复者ID */
    private Long replyUserId;

    /** TOP=顶级评论 USER_REPLY=用户回复 MERCHANT_REPLY=商家回复 */
    private String commentType;

    /** 评分 1-5（仅顶级评论） */
    private Integer rating;

    /** 评论内容 */
    private String content;

    /** 评论图片(JSON数组) */
    private String images;

    /** 1=正常 0=隐藏 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}