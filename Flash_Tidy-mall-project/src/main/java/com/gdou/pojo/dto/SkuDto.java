package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;

/**
 * SKU 创建/更新 DTO
 */
@Data
public class SkuDto {

    /**
     * 所属 SPU ID
     */
    @NotNull(message = "SPU ID 不能为空")
    private Long spuId;

    /**
     * SKU 名称（规格组合，如 "iPhone 15 黑色 256G"）
     */
    @NotBlank(message = "SKU 名称不能为空")
    @Size(max = 256, message = "SKU 名称最长 256 字")
    private String name;

    /**
     * 规格描述（JSON）
     */
    @Size(max = 1024, message = "规格描述最长 1024 字")
    private String spec;

    /**
     * 售价
     */
    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于 0")
    private BigDecimal price;

    /**
     * 原价/划线价
     */
    private BigDecimal originalPrice;

    /**
     * SKU 图片 URL
     */
    private String image;

    /**
     * 总库存
     */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    /**
     * 促销/秒杀库存配额
     */
    @Min(value = 0, message = "促销库存不能为负数")
    private Integer promotionStock;

    /**
     * 是否参与秒杀：0=否 1=是
     */
    private Integer isSeckill;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 秒杀开始时间
     */
    private Date seckillStartTime;

    /**
     * 秒杀结束时间
     */
    private Date seckillEndTime;

    /**
     * 排序值（越小越靠前）
     */
    private Integer sort;

    /**
     * 状态：0=下架 1=上架
     */
    private Integer status;
}
