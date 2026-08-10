package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.SkuDto;
import com.gdou.pojo.entity.Sku;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author huanghaiwei
* @description 针对表【sku(SKU 库存单元表（含秒杀 + 库存三字段）)】的数据库操作Service
* @createDate 2026-08-06 21:59:46
*/
public interface SkuService extends IService<Sku> {

    /**
     * 商家创建 SKU
     */
    Result createSku(Long merchantId, SkuDto skuDto);

    /**
     * 商家更新 SKU
     */
    Result updateSku(Long merchantId, Long skuId, SkuDto skuDto);

    /**
     * 商家查询某 SPU 下的所有 SKU
     */
    Result querySkuBySpu(Long merchantId, Long spuId);

    /**
     * 商家查询单个 SKU 详情
     */
    Result getSkuDetail(Long merchantId, Long skuId);

    /**
     * 商家删除 SKU
     */
    Result deleteSku(Long merchantId, Long skuId);

    /**
     * 商家上下架 SKU
     */
    Result updateSkuStatus(Long merchantId, Long skuId, Integer status);

    /**
     * 商家查看自己所有 SKU（跨 SPU）
     */
    Result queryAllMySkus(Long merchantId);

    /**
     * 用户端查询某 SPU 下的上架 SKU（无需登录）
     */
    Result userQuerySkusBySpu(Long spuId);

    /**
     * 商家查询自己的所有秒杀 SKU（含 SPU 名称、Redis 实时库存）
     */
    Result queryMySeckillSkus(Long merchantId);
}
