package com.gdou.pojo.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 管理员审核商家申请请求体
 */
@Data
public class MerchantReviewDto {
    @NotNull(message = "申请ID不能为空")
    private Long applicationId;

    @NotBlank(message = "审核结果不能为空")
    private String status;  // APPROVED / REJECTED

    private String comment;  // 审核备注（拒绝时建议填写原因）
}
