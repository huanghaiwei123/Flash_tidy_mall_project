package com.gdou.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 本地缓存caffeine是为了减缓redis的压力，避免无脑请求redis导致压力巨大，并且比guava cache性能跟好，也比redis速度更快
 * 适合读多写少的机制
 */
@Configuration
public class CaffeineConfig {
    @Bean("spuDetailCache")
    public Cache<String,String> spuDetailCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)   //最多一万个商品
                .expireAfterWrite(10, TimeUnit.MINUTES)   //写入十分钟后过期，保证数据一致性
                .recordStats()   //统计命中率，方便监控
                .build();
    }
}
