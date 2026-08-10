package com.gdou.controller;

import com.gdou.common.Result;
import com.gdou.service.CategoryService;
import com.gdou.service.MerchantService;
import com.gdou.service.SkuService;
import com.gdou.service.SpuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端 — 商品浏览（无需登录）
 */
@RestController
@RequestMapping("/hhw/goods")
@Slf4j
public class GoodsController {

    @Autowired
    private SpuService spuService;

    @Autowired
    private SkuService skuService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MerchantService merchantService;

    /**
     * 商品列表（支持分类筛选、关键词搜索、分页）
     *
     * @param categoryId 分类 ID（可选，含子分类）
     * @param keyword    搜索关键词（可选）
     * @param page       页码（默认 1）
     * @param size       每页条数（默认 20）
     */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size) {
        log.info("用户浏览商品：categoryId={}, keyword={}, page={}", categoryId, keyword, page);
        return spuService.userSpuList(categoryId, keyword, page, size);
    }

    /**
     * 商品详情（含 SKU 列表）
     */
    @GetMapping("/{spuId}")
    public Result detail(@PathVariable Long spuId) {
        log.info("用户查看商品详情：spuId={}", spuId);
        return spuService.userSpuDetail(spuId);
    }

    /**
     * 某商品下的 SKU 列表
     */
    @GetMapping("/{spuId}/skus")
    public Result skus(@PathVariable Long spuId) {
        return skuService.userQuerySkusBySpu(spuId);
    }

    /**
     * 分类树（用户端，只显示可见分类）
     */
    @GetMapping("/categories")
    public Result categories() {
        return categoryService.userQueryTree();
    }

    /**
     * 店铺主页（商家信息 + 商品列表）
     */
    @GetMapping("/shop/{merchantId}")
    public Result shop(@PathVariable Long merchantId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size) {
        log.info("用户浏览店铺：merchantId={}, page={}", merchantId, page);
        return merchantService.shopDetail(merchantId, page, size);
    }
}
