package com.gdou.controller.merchant;

import com.gdou.common.Result;
import com.gdou.pojo.dto.SkuDto;
import com.gdou.service.SkuService;
import com.gdou.util.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 商家 — SKU 管理
 */
@RestController
@RequestMapping("/hhw/sku")
@Slf4j
public class MerchantSkuController {

    @Autowired
    private SkuService skuService;

    /**
     * 创建 SKU
     */
    @PostMapping
    public Result create(@Valid @RequestBody SkuDto skuDto) {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 正在创建 SKU", merchantId);
        return skuService.createSku(merchantId, skuDto);
    }

    /**
     * 更新 SKU
     */
    @PutMapping("/{skuId}")
    public Result update(@PathVariable Long skuId,
                         @Valid @RequestBody SkuDto skuDto) {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 正在更新 SKU {}", merchantId, skuId);
        return skuService.updateSku(merchantId, skuId, skuDto);
    }

    /**
     * 查询某个 SPU 下的所有 SKU
     */
    @GetMapping("/spu/{spuId}")
    public Result listBySpu(@PathVariable Long spuId) {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 查询 SPU {} 的 SKU 列表", merchantId, spuId);
        return skuService.querySkuBySpu(merchantId, spuId);
    }

    /**
     * 查看自己所有 SKU（跨 SPU 汇总）
     */
    @GetMapping("/list")
    public Result listAll() {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 查询所有 SKU", merchantId);
        return skuService.queryAllMySkus(merchantId);
    }

    /**
     * 查询单个 SKU 详情
     */
    @GetMapping("/{skuId}")
    public Result detail(@PathVariable Long skuId) {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 查询 SKU {} 详情", merchantId, skuId);
        return skuService.getSkuDetail(merchantId, skuId);
    }

    /**
     * 删除 SKU
     */
    @DeleteMapping("/{skuId}")
    public Result delete(@PathVariable Long skuId) {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 正在删除 SKU {}", merchantId, skuId);
        return skuService.deleteSku(merchantId, skuId);
    }

    /**
     * 上下架 SKU
     */
    @PutMapping("/{skuId}/status")
    public Result toggleStatus(@PathVariable Long skuId,
                               @RequestParam Integer status) {
        Long merchantId = UserHolder.get();
        log.info("商家 {} 修改 SKU {} 状态为 {}", merchantId, skuId, status);
        return skuService.updateSkuStatus(merchantId, skuId, status);
    }
}
