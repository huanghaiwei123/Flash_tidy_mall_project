package com.gdou;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 集成测试 —— 需要 MySQL / Redis Cluster / RabbitMQ 全部就绪才能通过。
 * 本地开发时默认跳过，CI 环境或手动验证时取消 @Disabled 注解。
 */
@SpringBootTest
@Disabled("需要外部中间件 MySQL / Redis Cluster / RabbitMQ 就绪")
class HhwSeckillProjectApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 容器启动 + 所有 Bean 加载
    }

}
