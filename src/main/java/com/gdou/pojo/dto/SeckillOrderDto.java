package com.gdou.pojo.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class SeckillOrderDto {
    /** 商品名称（冗余，避免关联查询） */
    private String goodsName;

    /** 秒杀价格 */
    private BigDecimal price;
}
