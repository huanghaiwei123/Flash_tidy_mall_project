package com.gdou.pojo.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
@Data
public class SeckillOrderDto {
    /** 商品名称（冗余，避免关联查询） */
    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    /** 秒杀价格 */
    @Min(value=0,message = "秒杀价格最小为0")
    private BigDecimal price;
}
