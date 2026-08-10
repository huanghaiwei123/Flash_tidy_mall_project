package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.MerchantApplyDto;
import com.gdou.pojo.dto.MerchantReviewDto;

/**
 * 商家申请服务
 */
public interface MerchantApplicationService {

    /**
     * 用户提交商家申请
     */
    Result apply(Long userId, MerchantApplyDto dto);

    /**
     * 管理员审核
     */
    Result review(Long adminId, MerchantReviewDto dto);

    /**
     * 查询申请列表（管理员），默认查待审核
     */
    Result listApplications(String status);
}
