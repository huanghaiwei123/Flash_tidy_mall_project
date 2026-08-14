# AGENTS.md

## 项目结构

- Maven 多模块：根 `pom.xml` 是 `pom` 打包的聚合模块（无源码）。
  - `Flash_Tidy-mall-project/` — 真正的 Spring Boot 2.7.18 应用（artifactId `flash-mall`，包名 `com.gdou`）。
  - `captcha-spring-boot-starter/` — 应用依赖的内部 starter（通过 `META-INF/spring.factories` 自动装配）。
- `PROGRESS.md` — 开发日志/状态文档（中文）；修改业务逻辑前请先阅读。
- `Flash_Tidy-mall-project/seckill-performance-test-2026-08-12.md` — 秒杀压测结果。
- 过期内容：`Dockerfile` 和 `docker-compose.yml` 引用了已删除的 `seckill-app` 模块，不要使用它们；Docker 构建目前是坏的。
- `README.md` 为空；真正的文档都是中文。

## 构建与运行

- 目标是 Java 8（Spring Boot 父 POM 2.7.18）。本机安装的 JDK 是 23——编译可能会失败或报警；如果 `mvnw` 编译失败，请使用 JDK 8 工具链。
- 从根目录用 `.\mvnw.cmd install` 构建全部（reactor 会先构建 `captcha-spring-boot-starter`，再构建依赖它（版本 `0.0.1-SNAPSHOT`）的 `flash-mall`）。
- 快速编译检查：`.\mvnw.cmd -q -DskipTests package`。
- 应用入口：`com.gdou.FlashTidyMallProjectApplication`，端口 8080，Knife4j 文档在 `/doc.html`。
- 唯一的测试是 `@SpringBootTest` 的上下文加载测试。它需要可用的 MySQL（`localhost:3306/flash_mall`，root/123456）、位于 `192.168.147.131` 的 Redis + RabbitMQ，以及 Elasticsearch——没有这些服务应用无法启动。没有纯单元测试；验证改动 = 编译 + 对着整个技术栈运行。
- 数据库脚本：`src/main/resources/sql/mall.sql`（库 `flash_mall`，13 张表；注意 `order` 是保留字），另有 `insert_data.sql` 和 `seckill_demo.sql`。

## 约定

- 所有 REST 接口都以 `/hhw` 开头（见各 controller）。`WebConfig` 的 JWT 拦截器覆盖 `/**`，放行了 login/register/goods/pay-notify/pay-return。
- 入口类使用 `@MapperScan("com.gdou.mapper")`——不要改成对 `com.gdou.mapper` 用 `@ComponentScan`（之前这样做破坏过 Bean 注入，见 PROGRESS.md 2026-08-07）。
- 分层：`controller/` → `service/` 下的接口 + `service/impl/` 下的实现（`XxxServiceImpl`）→ MyBatis-Plus mapper，XML 在 `src/main/resources/com/gdou/mapper/`。响应统一用 `Result<T>`；错误通过 `BusinessException` + `GlobalExceptionHandler`（中文提示）处理。
- 实体都有 `deleted` 字段，接入了 MyBatis-Plus 逻辑删除。
- 秒杀热路径：Redis Lua（`resources/lua/seckill.lua`）原子扣减库存 + RabbitMQ 异步创建订单 + Guava 布隆过滤器。SKU 库存模型：`stock` / `locked_stock` / `available_stock` / `promotion_stock`（促销配额 ≤ 库存）。
- 已知拼写错误保持不变：`UseSeckillServiceImpl`（不是 `UserSeckillServiceImpl`）实现了 `UserSeckillService`。
- 提交信息和代码注释都用中文；保持这种风格。