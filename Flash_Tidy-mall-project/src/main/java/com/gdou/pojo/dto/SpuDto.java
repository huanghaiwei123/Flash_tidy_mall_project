package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SpuDto {
    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 128, message = "商品名称最长 128 字")
    private String name;

    /**
     * 商品描述
     */
    @Size(max = 512, message = "商品描述最长 512 字")
    private String description;

    /**
     * 所属分类ID（下拉框选中后传此值即可，无需传 categoryName）
     */
    private Long categoryId;

    /**
     * 所属分类名称（兼容旧前端手动输入，categoryId 优先）
     */
    private String categoryName;

    /**
     * 品牌
     */
    @Size(max = 64, message = "品牌名最长 64 字")
    private String brand;

    /**
     * 主图 URL
     */
    @NotBlank(message = "商品主图不能为空")
    private String mainImage;

    /**
     * 商品轮播图（JSON 数组）
     */
    private Object images;

    /**
     * 商品详情（富文本 HTML）
     */
    private String detail;
}
