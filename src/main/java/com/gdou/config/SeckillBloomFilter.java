package com.gdou.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

@Component
@Slf4j
public class SeckillBloomFilter {
    private volatile BloomFilter<Long> filter;

    private static final int EXPECTED_INSERTIONS = 10000; // 预期秒杀活动数量
    private static final double FPP = 0.02;                // 误判率 1%

    @PostConstruct
    public void init() {
        this.filter = BloomFilter.create(Funnels.longFunnel(), EXPECTED_INSERTIONS, FPP);
        log.info("布隆过滤器初始化完成，预期容量={}，误判率={}", EXPECTED_INSERTIONS, FPP);
    }

    /** 检查是否可能存在（false = 100% 不存在） */
    public boolean mightContain(Long seckillId) {
        return filter.mightContain(seckillId);
    }

    /** 单个添加 */
    public void add(Long seckillId) {
        filter.put(seckillId);
    }

}
