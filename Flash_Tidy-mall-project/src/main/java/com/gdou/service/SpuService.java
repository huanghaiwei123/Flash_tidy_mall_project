package com.gdou.service;

import com.gdou.common.Result;
import com.gdou.pojo.dto.SpuDto;
import com.gdou.pojo.entity.Spu;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author huanghaiwei
* @description 针对表【spu(商品 SPU 主表)】的数据库操作Service
* @createDate 2026-08-06 21:59:46
*/
public interface SpuService extends IService<Spu> {

    /** 商家查询自家商品 */
    Result spuQuery(Long merchantId);

    /** 商家上架商品 */
    Result spuSave(Long merchantId, SpuDto spuDto);

    /** 商家更新商品 */
    Result spuUpdate(Long merchantId, Long spuId, SpuDto spuDto);

    /** 商家下架/上架商品 */
    Result spuUpdateStatus(Long merchantId, Long spuId, Integer status);

    // ===================== 用户端 =====================

    /**
     * 用户端 SPU 列表（分类筛选 + 关键词搜索 + 分页）
     */
    Result userSpuList(Long categoryId, String keyword, Integer page, Integer size);

    /**
     * 用户端 SPU 详情（含 SKU 列表）
     */
    Result userSpuDetail(Long spuId);
}
