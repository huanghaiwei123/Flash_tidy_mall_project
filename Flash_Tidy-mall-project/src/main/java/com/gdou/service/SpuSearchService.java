package com.gdou.service;

import com.gdou.common.Result;

import java.util.Set;

/**
 * 商品搜索（Elasticsearch）—— 全量/增量同步 + 搜索
 */
public interface SpuSearchService {

    /** 全量导入：重建 spu 索引，写入所有上架 SPU */
    void fullSync();

    /** 增量同步：按 update_time 水印，同步变更的 SPU/SKU */
    void incrementalSync();

    /** 当前索引文档数（索引不存在返回 0） */
    long countDocs();

    /**
     * 搜索（分类过滤 + 关键词 + 分页）
     *
     * @param categoryIds 分类 ID 集合（含子分类，可为空）
     * @param keyword     关键词（可为空）
     */
    Result search(Set<Long> categoryIds, String keyword, Integer page, Integer size);

    /**
     * 精准搜索：只搜 name + brand，不搜 description，避免手机/耳机混搜
     */
    Result searchByName(String keyword, Integer page, Integer size);
}
