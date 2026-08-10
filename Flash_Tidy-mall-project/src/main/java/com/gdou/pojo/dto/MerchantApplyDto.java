package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 商家申请请求体
 */
@Data
public class MerchantApplyDto {
    @NotBlank(message = "店铺名称不能为空")
    @Size(min = 1, max = 64, message = "店铺名称最长 64 位")
    private String shopName;

    @Size(max = 255, message = "店铺简介最长 255 位")
    private String shopDescription;
}
