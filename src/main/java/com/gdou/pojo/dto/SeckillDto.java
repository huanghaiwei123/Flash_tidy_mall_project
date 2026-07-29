package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class SeckillDto {
    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /** 库存数量 */
    @Min(value = 0,message = "库存最小为0")
    private Integer number;

    /** 秒杀价格 */
    @Min(value = 0,message = "秒杀价格最小为0")
    private BigDecimal price;

    /** 原价 */
    @Min(value = 0,message = "原价最小为0")
    private BigDecimal originalPrice;

    /** 商品图片 */
    private String goodsImg;

    /** 秒杀开启时间 */
    @NotNull(message = "请明确标注秒杀相关时间")
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    @NotNull(message = "请明确标注秒杀相关时间")
    private LocalDateTime endTime;
}
