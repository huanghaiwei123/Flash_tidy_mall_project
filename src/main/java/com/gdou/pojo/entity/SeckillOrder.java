package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单表
 */
@Data
@TableName("seckill_order")
public class SeckillOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @TableId(type = IdType.AUTO)
    private Long orderId;

    /** 秒杀商品ID */
    private Long seckillId;

    /** 用户ID */
    private Long userId;

    /** 商品名称（冗余，避免关联查询） */
    private String goodsName;

    /** 秒杀价格 */
    private BigDecimal price;

    /**
     * 订单状态：
     * 1000-未支付
     * 1002-已支付
     * 1001-已取消
     * 1003-已退款
     */
    private Integer state;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;
}
