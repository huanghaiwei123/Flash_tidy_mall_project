package com.gdou.pojo.dto;
import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
@Data
public class SeckillOrderDto {
    @NotNull(message = "秒杀商品ID不能为空")
    private Long seckillId;    // 新增

    @NotNull(message = "用户ID不能为空")
    private Long userId;       // 新增

    /** 商品名称（冗余，避免关联查询） */
    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    /** 秒杀价格 */
    @Min(value=0,message = "秒杀价格最小为0")
    private BigDecimal price;
}
