package com.gdou.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀业务指标埋点
 */
@Component
public class SeckillMetrics {
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counterCache=new ConcurrentHashMap<>();
    public SeckillMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录秒杀结果
     * @param result
     */
    public void recordResult(String result){
        Counter counter = counterCache.computeIfAbsent(result, k -> meterRegistry.counter("seckill_order_total", "result", k));
        counter.increment();
    }

    /**
     * 记录lua扣库存耗时
     * @param runnable
     */
    public void recordLuaDuration(Runnable runnable){
        meterRegistry.timer("seckill_lua_duration_seconds").record(runnable);
    }

    /**
     * 记录 Lua 扣库存耗时（带返回值）
     */
    public <T> T recordLuaDuration(Callable<T> callable) throws Exception {
        return meterRegistry.timer("seckill_lua_duration_seconds").recordCallable(callable);
    }
}
