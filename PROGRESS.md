# 秒杀项目开发进度

> 最后更新：2026-08-04（Bug 修复 + bucket_id 完善 + 配置脱敏）

## 项目概况

- **项目名**：springboot-seckill-project
- **技术栈**：JDK 8 (Corretto) / SpringBoot 2.7.18 / MyBatis-Plus 3.5.5 / Redis (Lettuce) / MySQL / RabbitMQ / JWT (jjwt 0.12.5) / BCrypt / Knife4j 3.0.3 / 支付宝沙箱
- **包路径**：`com.gdou`
- **接口前缀**：`/hhw`

---

## 已完成 ✅

### 1. 项目骨架
- [x] SpringBoot 2.7.18 项目初始化 + 全部依赖
- [x] 配置文件 `application.yml`（MySQL + Redis + JWT 密钥 + MP 驼峰映射）
- [x] 启动类 `HhwSeckillProjectApplication`

### 2. 数据库设计（4 张表）
| 表名 | 实体类 | 说明 |
|------|--------|------|
| `seckill_user` | `SeckillUser` | 用户表（手机号登录，BCrypt 密码） |
| `seckill` | `Seckill` | 秒杀商品表（库存、价格、时间窗口、乐观锁 version） |
| `seckill_order` | `SeckillOrder` | 秒杀订单表（用户、商品、价格、状态、trade_no） |
| `user_seckill_record` | `UserSeckillRecord` | 用户秒杀记录表（唯一键 `uk_seckill_user` 防重） |

### 3. 基础架构层
- [x] **统一返回**：`Result<T>`
- [x] **常量类**：`ResultCodeConstant`、`ResultMessageConstant`、`RedisConstant`
- [x] **全局异常处理**：`GlobalExceptionHandle` + `BusinessException`
- [x] **枚举**：`SeckillEnum`
- [x] **MP 配置**：`MybatisplusConfig`（乐观锁插件 + 分页插件）
- [x] **Redis 配置**：`RedisConfig`（JSON 序列化 `RedisTemplate` + Lua 脚本 Bean）

### 4. 用户认证模块
- [x] **注册**：`RegisterController` → `RegisterService` → `RegisterServiceImpl`
- [x] **登录**：`LoginController` → `LoginService` → `LoginServiceImpl`
- [x] **JWT 工具类**：`JwtUtil`
- [x] **密码加密**：`PasswordEncoder`（BCrypt）
- [x] **JWT 拦截器**：`JwtInterceptor`（验证 token，存 userId 到 UserHolder）
- [x] **ThreadLocal**：`UserHolder`（存取清理）

### 5. 核心秒杀业务 ✅
- [x] **Controller**：`POST /hhw/seckill/onseckill/{seckillId}` + `GET` 列表/详情
- [x] **库存预热**：`POST /hhw/seckill/warmstock/{seckillId}`（Redis 加载库存）
- [x] **秒杀流程**（8 步）：
  1. 查商品是否存在
  2. 校验秒杀时间窗口
  3. **Redis Lua 原子操作**：防重 + DECR 预扣库存（`seckill.lua`）
  4. DB 原子扣库存：`UPDATE seckill SET number = number - 1 WHERE seckill_id = ? AND number > 0`
  5. 创建订单 `seckill_order`（state=1000 UNPAY）
  6. 创建秒杀记录 `user_seckill_record`（state=1000，唯一键防重）
  7. DB 失败 → Redis 回补库存 + 删除防重标记
- [x] **SeckillMapper.deductById**：原子扣库存 SQL

### 6. 支付宝电脑网站支付 ✅
- [x] **AlipayConfig**：`AlipayProperties` + `AlipayClient` Bean
- [x] **AlipayService**：生成支付页面 HTML / 验签 / 退款
- [x] **Pay 接口改造**：`pay()` 返回支付宝 HTML、新增 `refund()`、`notify()`、`return()`
- [x] **异步回调**：`/hhw/pay/notify` 验签 + 幂等处理 + 更新订单状态
- [x] **JWT 排除**：`/hhw/pay/notify`、`/hhw/pay/return` 无需登录
- [x] **数据库**：`seckill_order` 新增 `trade_no` 字段

### 7. 前端页面 ✅
- [x] `login.html`：登录 + 注册，Tab 切换
- [x] `index.html`：商品列表 + 秒杀 + 全部结果展示 + 支付/取消/退款操作
- [x] 秒杀结果自动加载全部记录（不再需要输入商品 ID）

### 8. MQ 异步下单 ✅
- [x] **RabbitMQ**：spring-boot-starter-amqp + Docker 部署
- [x] **消息体**：`SeckillMessage`（seckillId + userId + bucketId）
- [x] **拓扑**：DirectExchange → 业务队列 + 死信队列（失败回滚 Redis）
- [x] **Producer**：`SeckillMQSender` — Redis Lua 成功后发消息，立即返回
- [x] **Consumer**：`SeckillMQReceiver` — 异步 DB 扣库存 + 建订单 + 建记录
- [x] **DLQ 补偿**：最终失败 → 回滚 Redis 库存 + 删除防重标记

### 9. 接口限流 ✅
- [x] **`@RateLimit` 注解**：`limit` 次数 + `second` 时间窗口
- [x] **`RateLimitInterceptor`**：Redis INCR + TTL 实现滑动窗口限流
- [x] **`WebConfig`**：限流拦截器注册到 `/hhw/seckill/**`

### 10. 秒杀活动 CRUD ✅
- [x] **新增**：`POST /hhw/seckill` — 创建秒杀活动
- [x] **修改**：`PUT /hhw/seckill/{id}` — 修改秒杀活动
- [x] **删除**：`DELETE /hhw/seckill/{id}` — 删除秒杀活动

### 11. 修复的坑 ✅
- [x] `seckill_order.state`、`user_seckill_record.state` 列类型 TINYINT → INT（state=1000 超出范围）
- [x] RabbitMQ `guest` 用户远程登录拒绝 → 创建 `admin` 用户
- [x] Redis `bind 127.0.0.1` → `0.0.0.0` + `protected-mode no`
- [x] `application.yml` rabbitmq 缩进错误导致 host 未生效
- [x] 支付宝公钥填错（填成了应用公钥）
- [x] 退款逻辑：先改库再调支付宝，失败回滚 + 回补 Redis 库存
- [x] `SeckillTask` cron 注释与代码不一致

### 12. Redis Cluster + 库存分桶 ✅
- [x] 搭建 Redis Cluster（3 主 3 从，端口 7000-7005）
- [x] `application.yml` 单机配置 → 集群配置（`spring.redis.cluster.nodes`）
- [x] 所有 key 加 hash tag `{seckillId}`，保证同一秒杀活动的 key 落在同一 slot
- [x] `SeckillTask.autoWarmStock`：库存分 10 桶预热（`seckill_stock:{id}:0` ~ `:9`）
- [x] `seckill.lua` 重写：Fisher-Yates 洗牌后随机选桶，返回桶编号 1~10
- [x] `SeckillServiceImpl.onSeckill`：传 11 个 KEYS + 解析 bucketId + 发送 MQ 消息
- [x] `SeckillMqReceiver.onDeadMessage`：回滚精确到具体桶（`INCR seckill_stock:{id}:{bucketId}`）

### 13. 订单超时取消 ✅
- [x] `SeckillMessage` 新增 `orderId` 字段
- [x] `RabbitMqConfig`：新增延迟队列拓扑（`order.cancel.delay.queue` TTL 15min → `order.cancel.dead.queue`）
- [x] `SeckillMapper.incrementById`：取消时回补 DB 库存
- [x] `SeckillMqSender.sendCancelDelay`：发送延迟取消消息
- [x] `SeckillMqReceiver.onMessage`：创建订单后发延迟消息
- [x] `SeckillMqReceiver.onCancelMessage`：消费超时消息，检查 UNPAY → 取消 + 回补库存

### 14. 布隆过滤器防缓存穿透 ✅
- [x] 添加 Guava 依赖
- [x] `SeckillBloomFilter`：预期 10000 条，误判率 1%
- [x] `SeckillTask` 启动时全量加载 + 定时预热时追加
- [x] `SeckillServiceImpl.onSeckill`：布隆过滤器快速拦截不存在的 ID

### 15. 性能压测与优化 ✅
- [x] `SeckillLoadTest`：自研 Java 压测工具
- [x] JMeter 5.6.3：标准化压测（聚合报告）
- [x] Ramp-Up=0 极限压测：单机峰值 **1131 QPS / 0 异常**
- [x] 诊断并修复 5 个瓶颈（Redis 连接池 / Tomcat 线程 / MQ 异步 / 限流 Lua 序列化 / 客户端连接数）

#### 压测结果（JMeter Ramp-Up=0）

| 线程数 | 异常率 | 平均响应 | 最大响应 | QPS |
|:---:|:---:|:---:|:---:|:---:|
| 100 | 0% | 49 ms | 86 ms | 885 |
| 200 | 0% | 74 ms | 140 ms | 1036 |
| 300 | 0% | 84 ms | 181 ms | 1075 |
| 500 | 0% | 84 ms | 201 ms | **1131** |

> **单机极限 ~1131 QPS**。核心链路：Redis Lua 原子扣库存 → MQ 异步落库，平均响应 84ms。架构正确，瓶颈在 SpringMVC 框架层，扩容可线性提升。

### 16. 秒杀 URL 动态化 ✅
- [x] `GET /hhw/seckill/getToken/{seckillId}`：返回 5 秒有效的一次性随机 token
- [x] `POST /hhw/seckill/onseckill/{seckillId}?token=xxx`：先校验 token 再秒杀
- [x] 前端两步秒杀：先拿 token → 带 token 秒杀
- [x] **防脚本原理**：token 是秒杀瞬间才下发的，脚本无法提前知道完整 URL

#### 瓶颈分析

| 瓶颈 | 原因 | 修复 |
|------|------|------|
| **Redis 连接池耗尽** | `max-active=8`，500 并发抢 8 连接，大量排队超时 | `max-active→200, max-idle→50, min-idle→10` |
| **Tomcat 线程不足** | 默认 max=200, accept=100，500 并发超出直接拒绝 | `server.tomcat.threads.max→1000, accept-count→2000` |
| **MQ 同步发送阻塞** | `convertAndSend()` 等 broker ack，浪费请求线程 | `@EnableAsync` + `@Async("mqExecutor")` 异步发送 |
| **限流器双次 Redis** | INCR + EXPIRE 两次网络往返 | 合并为 `ratelimit.lua` 一次 EVAL |
| **限流器 Lua 序列化错误** | `Jackson2JsonRedisSerializer` 带类型信息序列化 Integer，Lua `tonumber()` 无法解析 | `RateLimitInterceptor` 改用 `StringRedisTemplate` + `String.valueOf()` |

#### 改动文件

| 文件 | 改动 |
|------|------|
| `application.yml` | Redis 连接池扩容 + Tomcat 线程池打开 |
| `HhwSeckillProjectApplication.java` | 加 `@EnableAsync` |
| `RabbitMqConfig.java` | 加 `mqExecutor` 线程池 Bean |
| `SeckillMqSender.java` | `send()` 加 `@Async("mqExecutor")` |
| `lua/ratelimit.lua` | **新建**：限流 INCR+EXPIRE 合并为一次 Redis 往返 |
| `RedisConfig.java` | 注册 `RateLimitScript` Bean |
| `RateLimitInterceptor.java` | 改用 `StringRedisTemplate` + Lua 脚本 |
| `SeckillLoadTest.java` | 加 `http.maxConnections` + 可调并发/请求数 |
| `pom.xml` | 加 `maven-compiler-plugin` 注解处理器（Lombok 支持 Maven 编译） |

---

## 进行中 🔄

暂无

---

## 2026-08-04：Bug 修复 + 配置脱敏 + bucket_id 完善

### 配置脱敏（GitHub 上线准备） ✅
- [x] `application.yml`：MySQL/Redis/RabbitMQ 的 IP、密码、JWT 密钥、支付宝密钥/回调 URL 全部替换为占位符
- [x] `docker-compose.yml`：同上脱敏
- [x] `index.html` / `login.html`：`localhost:8080` → 占位符
- [x] `PayController.java`：硬编码 `localhost:8080` → 占位符

### Bug 修复 ✅

| # | 严重度 | 文件 | 问题 | 修复 |
|:---:|:---:|------|------|------|
| 1 | 🔴 | `PayServiceImpl.cancel()` | Redis 库存回补 key 缺桶编号后缀，库存回不到正确桶 | 新增 `bucket_id` 字段 + 精确回滚 |
| 2 | 🔴 | `RegisterServiceImpl.register()` | LambdaQueryWrapper 复用导致昵称检查变成 AND 条件 | 新建独立 wrapper |
| 3 | 🟡 | `CaptchaService.java` | 验证码尺寸硬编码，CaptchaProperties 配置无效 | 注入配置 Bean |
| 4 | 🟡 | `SeckillServiceImpl.updateSeckill()` | BeanUtils.copyProperties 可能覆盖 version | 显式 ignore 字段 |
| 5 | 🟡 | `SeckillUserMapper.xml` | 引用不存在的 salt 列 | 删除 salt 映射 |
| 6 | 🟡 | `seckill.sql` | 残留 `desc seckill_order;` 调试语句 | 删除 |
| 7 | 🟡 | `SeckillOrder` / `UserSeckillRecord` | @Builder 缺 @NoArgsConstructor | 补充注解 |
| 8 | 🟡 | `SeckillOrderMapper.xml` | 漏了 `trade_no` 列映射 | 补全 |
| 9 | 🟢 | `GlobalExceptionHandle` | 通用异常不打印堆栈 | `log.error("Unexpected error", e)` |
| 10 | 🟢 | `SeckillTask` | 日志判断变量 b 在循环中被覆盖 | 改为 anyNew 布尔累积 |

### 架构改进 ✅
- [x] `seckill_order` 表新增 `bucket_id` 列，订单持久化桶编号
- [x] `SeckillMqReceiver.onMessage()` 创建订单时写入 bucketId
- [x] `PayServiceImpl.cancel()` 从订单读取 bucketId 精确回滚 Redis 库存桶
- [x] MQ 超时取消 → 精确回滚；手动取消 → 精确回滚（之前是随机桶）

### 下一步计划
1. 分布式事务：MQ 消费端幂等 + 数据一致性（RocketMQ 事务消息 或 Seata）
2. 优惠券系统：复用 Redis 分桶 + Lua + MQ 架构
3. 微服务拆分：Nacos + Gateway + OpenFeign

---

## 接口总览

| 方法 | 路径 | 说明 | 登录 |
|:---:|------|------|:---:|
| POST | `/hhw/login` | 登录 | ❌ |
| POST | `/hhw/register` | 注册 | ❌ |
| GET | `/hhw/seckill/seckills` | 商品列表 | ❌ |
| GET | `/hhw/seckill/{id}` | 商品详情 | ✅ |
| POST | `/hhw/seckill` | 创建秒杀活动 | ✅ |
| PUT | `/hhw/seckill/{id}` | 修改秒杀活动 | ✅ |
| DELETE | `/hhw/seckill/{id}` | 删除秒杀活动 | ✅ |
| GET | `/hhw/seckill/getToken/{id}` | 获取秒杀令牌 🔑 | ✅ |
| POST | `/hhw/seckill/onseckill/{id}?token=` | 执行秒杀 ⚡ | ✅ |
| GET | `/hhw/seckill/seckillResult` | 我的秒杀结果列表 | ✅ |
| POST | `/hhw/pay/{orderId}` | 支付宝支付（返回 HTML） | ✅ |
| POST | `/hhw/pay/cancel/{orderId}` | 取消支付 | ✅ |
| POST | `/hhw/pay/refund/{orderId}` | 退款 | ✅ |
| POST | `/hhw/pay/notify` | 支付宝异步回调 | ❌ RSA2 |
| GET | `/hhw/pay/return` | 支付同步跳转 | ❌ |

---

## 关键文件索引

```
src/main/java/com/gdou/
├── HhwSeckillProjectApplication.java
├── common/Result.java
├── Constant/（RedisConstant, ResultCodeConstant, ResultMessageConstant）
├── Enum/SeckillEnum.java
├── limit/RateLimit.java
├── config/
│   ├── WebConfig.java                 # 拦截器配置
│   ├── MybatisplusConfig.java         # MP 插件
│   ├── RedisConfig.java               # Redis 序列化 + Lua Bean
│   ├── AlipayConfig.java              # AlipayClient Bean
│   ├── AlipayProperties.java          # 支付宝配置属性
│   ├── RabbitMqConfig.java            # MQ 拓扑 + 延迟取消队列
│   └── SeckillBloomFilter.java        # 布隆过滤器防穿透
├── interceptor/（JwtInterceptor, RateLimitInterceptor）
├── exception/（BusinessException, GlobalExceptionHandle）
├── util/（JwtUtil, PasswordEncoder, UserHolder）
├── controller/
│   ├── RegisterController.java
│   ├── LoginController.java
│   ├── SeckillController.java
│   └── PayController.java
├── service/
│   ├── RegisterService / LoginService / SeckillService
│   ├── SeckillOrderService / UserSeckillRecordService
│   ├── PayService / AlipayService
│   └── impl/（全部实现类）
├── mapper/（SeckillMapper 含 deductById）
├── pojo/
│   ├── entity/（SeckillUser, Seckill, SeckillOrder, UserSeckillRecord）
│   └── dto/（LoginDto, RegisterDto, SeckillDto, SeckillOrderDto）
├── mq/（SeckillMessage, SeckillMqSender, SeckillMqReceiver）
└── task/SeckillTask.java
src/main/resources/
├── application.yml
├── lua/seckill.lua
├── sql/seckill.sql
└── static/（login.html, index.html）
```
