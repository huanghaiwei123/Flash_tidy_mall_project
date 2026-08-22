package com.gdou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.common.Result;
import com.gdou.mapper.SkuMapper;
import com.gdou.mapper.SpuMapper;
import com.gdou.pojo.dto.SpuDto;
import com.gdou.pojo.entity.Sku;
import com.gdou.pojo.entity.Spu;
import com.gdou.pojo.es.SpuDoc;
import com.gdou.pojo.vo.SpuListVo;
import com.gdou.service.SpuSearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class SpuSearchServiceImpl implements SpuSearchService {
@Autowired
private ElasticsearchOperations operations;
@Autowired
private SpuMapper spuMapper;
@Autowired
private SkuMapper skuMapper;

//时间水印，记录上一次更新的时间
private volatile Date lastSyncTime;
//    全量同步
    @Override
    public void fullSync() {
        IndexOperations indexOps = operations.indexOps(SpuDoc.class);
        if(indexOps.exists()) {
            indexOps.delete();
        }
        LambdaQueryWrapper<Spu> spuLambdaQueryWrapper = new LambdaQueryWrapper<>();
        spuLambdaQueryWrapper.eq(Spu::getStatus, 1);
        List<Spu> spuList = spuMapper.selectList(spuLambdaQueryWrapper);
        LambdaQueryWrapper<Sku> skuLambdaQueryWrapper = new LambdaQueryWrapper<>();
        skuLambdaQueryWrapper.eq(Sku::getStatus, 1);
        List<Sku> skuList = skuMapper.selectList(skuLambdaQueryWrapper);
        Map<Long, List<Sku>> listMap = skuList.stream().filter(s -> s.getSpuId() != null).collect(Collectors.groupingBy(Sku::getSpuId));
        List<SpuDoc> spuDocList=new ArrayList<>();
        for(Spu spu: spuList) {
            double minPrice = Double.MAX_VALUE;
            List<Sku> skus = listMap.getOrDefault(spu.getId(), Collections.emptyList());
            if(!skus.isEmpty()) {
                for(Sku sku: skus) {
                   minPrice=Math.min(minPrice,sku.getPrice().doubleValue());
                }
            }
            SpuDoc spuDoc = new SpuDoc();
            BeanUtils.copyProperties(spu,spuDoc);
//            判断商品是否有规格
            minPrice=skus.isEmpty()?0.1:minPrice;
            spuDoc.setMinPrice(minPrice);
            spuDoc.setSkuCount(skus.size());
            spuDocList.add(spuDoc);
        }

        operations.save(spuDocList);
        indexOps.refresh();
        log.info("ES商品全量同步成功");
    }

//    局部增量
    @Override
    public void incrementalSync() {
        Date now = new Date();
        if(lastSyncTime == null) {
//            项目启动第一次增量是全局增量，只要加个时间水印就可以了
            lastSyncTime = now;
            return;
        }
//        上一次更新时间水印前推一分钟
        Date since = new Date(lastSyncTime.getTime() - 1 * 60 * 1000);
        lastSyncTime = now;
//        防止sku更改但是spu没改
        LambdaQueryWrapper<Spu> spuLambdaQueryWrapper1 = new LambdaQueryWrapper<>();
        spuLambdaQueryWrapper1.gt(Spu::getUpdateTime, since);
        List<Spu> spuList = spuMapper.selectList(spuLambdaQueryWrapper1);
        LambdaQueryWrapper<Sku> skuLambdaQueryWrapper = new LambdaQueryWrapper<>();
        skuLambdaQueryWrapper.gt(Sku::getUpdateTime, since);
        List<Sku> skuList = skuMapper.selectList(skuLambdaQueryWrapper);
        Map<Long, List<Sku>> listMap = skuList.stream().filter(s -> s.getSpuId() != null).collect(Collectors.groupingBy(Sku::getSpuId));
        Set<Map.Entry<Long, List<Sku>>> entries = listMap.entrySet();
        Set<Spu> spuSet=new HashSet<>();
        for(Map.Entry<Long, List<Sku>> entry: entries) {
            Long spuId = entry.getKey();
            Spu spu = spuMapper.selectById(spuId);
            spuSet.add(spu);
        }
        spuSet.addAll(spuList);
        List<SpuDoc> spuDocList=new ArrayList<>();
        for(Spu spu: spuSet) {
            if(spu.getStatus()==0){
                SpuDoc spuDoc = new SpuDoc();
                spuDoc.setId(spu.getId());
                operations.delete(spuDoc);
                continue;
            }
            double minPrice = Double.MAX_VALUE;
            LambdaQueryWrapper<Sku> skuLambdaQueryWrapper1 = new LambdaQueryWrapper<Sku>().eq(Sku::getStatus, 1).eq(Sku::getSpuId, spu.getId());
            List<Sku> list = skuMapper.selectList(skuLambdaQueryWrapper1);
            for(Sku sku: list) {
                minPrice=Math.min(minPrice,sku.getPrice().doubleValue());
            }
            SpuDoc spuDoc = new SpuDoc();
            BeanUtils.copyProperties(spu,spuDoc);
            minPrice=list.isEmpty()?0.0:minPrice;
            spuDoc.setMinPrice(minPrice);
            spuDoc.setSkuCount(list.size());
            spuDocList.add(spuDoc);
        }
        operations.save(spuDocList);
    }

//    计算文档数量
    @Override
    public long countDocs() {
        IndexOperations indexOps = operations.indexOps(SpuDoc.class);
        if(!indexOps.exists()) {
            return 0;
        }
        return operations.count(new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).build(),SpuDoc.class);
    }

//    搜索
    @Override
    public Result search(Set<Long> categoryIds, String keyword, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        // 布尔查询：filter 不影响打分（做过滤），must 参与打分（做匹配）
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.termQuery("status", 1));          // 只查上架
        if (categoryIds != null && !categoryIds.isEmpty()) {
            bool.filter(QueryBuilders.termsQuery("categoryId", categoryIds)); // 分类过滤
        }
        if (StringUtils.hasText(keyword)) {
            bool.must(QueryBuilders.multiMatchQuery(keyword.trim(), "name", "brand", "description"));
        }

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(bool)
                .withSorts(
                        SortBuilders.fieldSort("sales").order(SortOrder.DESC),
                        SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(page - 1, size))  // ES 页码从 0 开始
                .build();

        SearchHits<SpuDoc> hits = operations.search(query, SpuDoc.class);

        List<SpuListVo> voList = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toVo)
                .collect(Collectors.toList());

        // 返回结构与原来 /goods/list 完全一致，前端零改动
        Map<String, Object> result = new HashMap<>();
        result.put("total", hits.getTotalHits());
        result.put("page", page);
        result.put("size", size);
        result.put("records", voList);
        return Result.success(result);

    }

//    精准搜索：只搜 name + brand，避免手机/耳机混搜
    @Override
    public Result searchByName(String keyword, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.termQuery("status", 1));
        if (StringUtils.hasText(keyword)) {
            bool.must(QueryBuilders.multiMatchQuery(keyword.trim(), "name", "brand"));
        }

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(bool)
                .withSorts(
                        SortBuilders.fieldSort("sales").order(SortOrder.DESC),
                        SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(page - 1, size))
                .build();

        SearchHits<SpuDoc> hits = operations.search(query, SpuDoc.class);

        List<SpuListVo> voList = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toVo)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", hits.getTotalHits());
        result.put("page", page);
        result.put("size", size);
        result.put("records", voList);
        return Result.success(result);
    }

// ==================== 内部工具方法 ====================

    /** SpuDoc -> SpuListVo（minPrice 从 Double 转回 BigDecimal） */
    private SpuListVo toVo(SpuDoc doc) {
        SpuListVo vo = new SpuListVo();
        vo.setId(doc.getId());
        vo.setName(doc.getName());
        vo.setDescription(doc.getDescription());
        vo.setCategoryName(doc.getCategoryName());
        vo.setBrand(doc.getBrand());
        vo.setMainImage(doc.getMainImage());
        vo.setSales(doc.getSales());
        vo.setStatus(doc.getStatus());
        vo.setMinPrice(doc.getMinPrice() == null ? BigDecimal.ZERO : BigDecimal.valueOf(doc.getMinPrice()));
        vo.setSkuCount(doc.getSkuCount());
        return vo;
    }


}