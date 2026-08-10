package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券创建/更新 DTO
 */
@Data
public class CouponDto {
    /**
     * 优惠券名称
     */
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 128, message = "优惠券名称最长 128 字")
    private String name;

    /**
     * 类型：FULL_REDUCTION=满减 DISCOUNT=折扣
     */
    @NotBlank(message = "优惠券类型不能为空")
    @Pattern(regexp = "FULL_REDUCTION|DISCOUNT", message = "类型只能为 FULL_REDUCTION 或 DISCOUNT")
    private String type;

    /**
     * 优惠值（满减=减多少元；折扣=0.85即85折）
     */
    @NotNull(message = "优惠值不能为空")
    @DecimalMin(value = "0.01", message = "优惠值必须大于 0")
    private BigDecimal discountValue;

    /**
     * 最低消费金额（0=无门槛）
     */
    @DecimalMin(value = "0.00", message = "最低消费金额不能小于 0")
    private BigDecimal minAmount;

    /**
     * 发放总量
     */
    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少为 1")
    private Integer totalCount;

    /**
     * 每人限领数量
     */
    @Min(value = 1, message = "每人限领至少为 1")
    private Integer perUserLimit;

    /**
     * 有效期开始
     */
    @NotNull(message = "有效期开始时间不能为空")
    private Date startTime;

    /**
     * 有效期结束
     */
    @NotNull(message = "有效期结束时间不能为空")
    private Date endTime;

    /**
     * 状态：0=禁用 1=正常
     */
    private Integer status;
}
