package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 发表评论入参
 */
@Data
public class ProductCommentDto {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long spuId;

    /** SKU ID（可选，买过可评具体规格） */
    private Long skuId;

    /** 关联订单号（可选，买过才传） */
    private String orderNo;

    /** 父评论ID，null=顶级评论 */
    private Long parentId;

    /** TOP=顶级评论 USER_REPLY=用户回复 MERCHANT_REPLY=商家回复 */
    private String commentType;

    /** 评分 1-5（仅顶级评论必填） */
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer rating;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 评论图片(JSON数组，可选) */
    private String images;
}