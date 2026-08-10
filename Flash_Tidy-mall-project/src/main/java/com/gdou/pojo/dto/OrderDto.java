package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class OrderDto {

    /**
     * 收货地址快照（下单时冗余存储，不受地址变更影响）
     */
    @NotNull(message = "收货地址不能为空")
    private Object addressSnapshot;

    /**
     *
     */
    @NotEmpty(message = "购物车不能为空")
    private List<ShoppingCarDto> car;

    /**
     * 优惠卷id
     */
    private Long couponId;
}
