package com.gdou.pojo.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class SeckillDto {
    /** 商品名称 */
    private String name;

    /** 库存数量 */
    private Integer number;

    /** 秒杀价格 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 商品图片 */
    private String goodsImg;

    /** 秒杀开启时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;
}
