package com.gdou.controller.admin;

import com.gdou.common.Result;
import com.gdou.pojo.dto.MerchantReviewDto;
import com.gdou.service.MerchantApplicationService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 管理员 — 商家申请审核
 */
@RestController
@RequestMapping("/hhw/admin/merchant")
@Slf4j
public class MerchantController {

    @Autowired
    private MerchantApplicationService merchantApplicationService;

    /**
     * 查看商家申请列表
     * @param status 可选：PENDING（默认）/ APPROVED / REJECTED
     */
    @GetMapping("/applications")
    public Result listApplications(@RequestParam(required = false) String status) {
        log.info("管理员查询商家申请列表，状态: {}", status);
        return merchantApplicationService.listApplications(status);
    }

    /**
     * 审核商家申请
     */
    @PostMapping("/review")
    public Result review(@Valid @RequestBody MerchantReviewDto dto) {
        Long adminId = UserHolder.get();
        log.info("管理员{}审核申请#{}，结果: {}", adminId, dto.getApplicationId(), dto.getStatus());
        return merchantApplicationService.review(adminId, dto);
    }
}
