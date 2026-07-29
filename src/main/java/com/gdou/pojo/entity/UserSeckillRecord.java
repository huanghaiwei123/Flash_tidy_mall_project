package com.gdou.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户秒杀记录表
 */
@Data
@TableName("user_seckill_record")
@Builder
public class UserSeckillRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 秒杀商品ID */
    private Long seckillId;

    /** 用户ID */
    private Long userId;

    /** 关联订单ID */
    private Long orderId;

    /**
     * 秒杀状态：
     * 1000-秒杀成功待支付
     * 1002-支付成功
     * 605-秒杀失败
     */
    private Integer state;

    /** 创建时间 */
    private LocalDateTime createTime;
}
