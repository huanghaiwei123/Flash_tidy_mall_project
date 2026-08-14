package com.gdou.controller.merchant;

import com.gdou.common.Result;
import com.gdou.service.SkuService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 商家 — 秒杀活动管理
 */
@RestController
@RequestMapping("/hhw/merchant/seckill")
@Slf4j
public class MerchantSeckillController {

    @Autowired
    private SkuService skuService;

    /**
     * 查看自己所有秒杀活动（含实时库存）
     */
    @GetMapping("/list")
    public Result list() {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 查看秒杀活动列表", merchantId);
        return skuService.queryMySeckillSkus(merchantId);
    }
}
