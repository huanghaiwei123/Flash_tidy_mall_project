package com.gdou.task;

import com.gdou.service.SpuSearchService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class EsSyncTask {
    @Autowired
    private SpuSearchService spuSearchService;

    @PostConstruct
    public void init() {
        MDC.put("traceId", "task-es-init-" + System.currentTimeMillis());
        try {
            long count = spuSearchService.countDocs();
            if (count > 0) {
                log.info("当前ES已有文档，跳过全量同步");
            } else {
                spuSearchService.fullSync();
                log.info("当前ES没有数据，执行全量同步");
            }
        }catch (Exception e){
            log.warn("ES 初始化失败: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @Scheduled(fixedRate = 60*1000)
    public void sync(){
        MDC.put("traceId", "task-es-sync-" + System.currentTimeMillis());
        try {
            spuSearchService.incrementalSync();
        } catch (Exception e) {
            log.warn("ES 增量同步失败: {}", e.getMessage());
        } finally {
            MDC.clear();
        }
    }

}
