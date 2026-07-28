package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品表
 */
@Data
@TableName("seckill")
public class Seckill implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品库存id */
    @TableId(type = IdType.AUTO)
    private Long seckillId;

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

    /** 创建时间 */
    private LocalDateTime createTime;

    @Version
    /** 版本号（乐观锁） */
    private Integer version;
}
