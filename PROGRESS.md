# 秒杀项目开发进度

> 最后更新：2026-07-30

## 项目概况

- **项目名**：springboot-seckill-project
- **技术栈**：JDK 8 (Corretto) / SpringBoot 2.7.18 / MyBatis-Plus 3.5.5 / Redis (Lettuce) / MySQL / JWT (jjwt 0.12.5) / BCrypt / Knife4j 3.0.3
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
| `seckill_order` | `SeckillOrder` | 秒杀订单表（用户、商品、价格、状态） |
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

### 6. Mock 支付模块 ✅
- [x] **支付**：`POST /hhw/pay/{orderId}` → `UNPAY → PAY_SUCCESS`
- [x] **取消**：`POST /hhw/pay/cancel/{orderId}` → `UNPAY → PAY_CANCEL` + Redis 回补库存

### 7. 秒杀结果查询 ✅
- [x] `GET /hhw/seckill/seckillResult/{seckillId}` → 查 `user_seckill_record`

### 8. 接口限流 ✅
- [x] **`@RateLimit` 注解**：`limit` 次数 + `second` 时间窗口
- [x] **`RateLimitInterceptor`**：Redis INCR + TTL 实现滑动窗口限流
- [x] **`WebConfig`**：限流拦截器注册到 `/hhw/seckill/**`

### 9. 代码清理 ✅
- [x] `seckillResult` 接口改为 GET
- [x] 清理无用 import（17 个文件，28 条 import）

### 10. 秒杀活动 CRUD ✅
- [x] **新增**：`POST /hhw/seckill` — 创建秒杀活动（SeckillDto 校验）
- [x] **修改**：`PUT /hhw/seckill/{id}` — 修改秒杀活动属性
- [x] **删除**：`DELETE /hhw/seckill/{id}` — 删除秒杀活动
- [x] 仅操作 DB，不涉及 Redis 预热（预热由定时任务负责）

### 11. MQ 异步下单 ✅
- [x] **RabbitMQ**：spring-boot-starter-amqp + Docker 部署
- [x] **消息体**：`SeckillMessage`（seckillId + userId）
- [x] **拓扑**：DirectExchange → 业务队列 + 死信队列（失败回滚 Redis）
- [x] **Producer**：`SeckillMQSender` — Redis Lua 成功后发消息，立即返回
- [x] **Consumer**：`SeckillMQReceiver` — 异步 DB 扣库存 + 建订单 + 建记录
- [x] **DLQ 补偿**：最终失败 → 回滚 Redis 库存 + 删除防重标记

---

## 接口总览

| 方法 | 路径 | 说明 | 登录 |
|:---:|------|------|:---:|
| **POST** | `/hhw/seckill` | **新增** 创建秒杀活动 | ✅ |
| GET | `/hhw/seckill/seckills` | 商品列表 | ❌ |
| GET | `/hhw/seckill/{id}` | 商品详情 | ✅ |
| **PUT** | `/hhw/seckill/{id}` | **新增** 修改秒杀活动 | ✅ |
| **DELETE** | `/hhw/seckill/{id}` | **新增** 删除秒杀活动 | ✅ |
| POST | `/hhw/seckill/onseckill/{id}` | 执行秒杀 ⚡ | ✅ |
| GET | `/hhw/seckill/seckillResult/{id}` | 秒杀结果 | ✅ |
| POST | `/hhw/pay/{orderId}` | Mock 支付 | ✅ |
| POST | `/hhw/pay/cancel/{orderId}` | 取消支付 | ✅ |
| POST | `/hhw/login` | 登录 | ❌ |
| POST | `/hhw/register` | 注册 | ❌ |

---

## 待完成 ❌

- [ ] 可能的增强：WeChat Pay 真实支付、退款

---

## 关键文件索引

```
src/main/java/com/gdou/
├── HhwSeckillProjectApplication.java        # 启动类
├── common/Result.java                       # 统一返回
├── Constant/
│   ├── ResultCodeConstant.java              # 状态码常量
│   ├── ResultMessageConstant.java           # 消息常量
│   └── RedisConstant.java                   # Redis key 常量
├── Enum/SeckillEnum.java                    # 秒杀枚举
├── limit/RateLimit.java                     # 限流注解
├── annotation/（限流注解，位于 limit 包）
├── config/
│   ├── WebConfig.java                       # 拦截器配置（JWT + 限流）
│   ├── MybatisplusConfig.java               # MP 乐观锁 + 分页插件
│   └── RedisConfig.java                     # Redis 序列化 + Lua 脚本 Bean
├── interceptor/
│   ├── JwtInterceptor.java                  # JWT 鉴权拦截器
│   └── RateLimitInterceptor.java            # 限流拦截器
├── exception/
│   ├── BusinessException.java               # 业务异常
│   └── GlobalExceptionHandle.java           # 全局异常处理
├── util/
│   ├── JwtUtil.java                         # JWT 工具
│   ├── PasswordEncoder.java                 # BCrypt 密码工具
│   └── UserHolder.java                      # ThreadLocal 当前用户ID
├── controller/
│   ├── RegisterController.java              # 注册
│   ├── LoginController.java                 # 登录
│   ├── SeckillController.java               # 秒杀 + 预热 + 结果查询
│   └── PayController.java                   # 支付 + 取消
├── service/
│   ├── RegisterService.java / impl/
│   ├── LoginService.java / impl/
│   ├── SeckillUserService.java / impl/
│   ├── SeckillService.java / impl/          # 秒杀核心逻辑
│   ├── SeckillOrderService.java / impl/
│   ├── UserSeckillRecordService.java / impl/ # 秒杀记录 + 结果查询
│   └── PayService.java / impl/              # 支付 + 取消
├── mapper/（SeckillMapper 含 deductById 原子SQL）
├── pojo/
│   ├── entity/（SeckillUser, Seckill, SeckillOrder, UserSeckillRecord）
│   └── dto/（LoginDto, RegisterDto, SeckillDto, SeckillOrderDto）
└── resources/
    ├── application.yml
    ├── lua/seckill.lua                       # Redis Lua 脚本
    └── sql/seckill.sql
```
