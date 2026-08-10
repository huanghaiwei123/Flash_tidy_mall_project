package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 购物车添加/更新请求 DTO
 */
@Data
public class ShoppingCarDto {
    /**
     * SPU ID
     */
    @NotNull(message = "spuId 不能为空")
    private Long spuId;

    /**
     * SKU ID
     */
    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    /**
     * 商家id
     */
    @NotNull(message = "merchantId 不能为空")
    private Long merchantId;

    /**
     * 数量（最低 1）
     */
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;
}
