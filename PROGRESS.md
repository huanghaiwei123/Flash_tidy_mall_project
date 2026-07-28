# 秒杀项目开发进度

> 最后更新：2026-07-28 晚（会话待续）

## 项目概况

- **项目名**：springboot-seckill-project
- **技术栈**：JDK 8 / SpringBoot 2.7.18 / MyBatis-Plus 3.5.5 / Redis (Lettuce) / MySQL / JWT (jjwt 0.12.5) / BCrypt / Knife4j 3.0.3
- **包路径**：`com.gdou`
- **接口前缀**：`/hhw`

---

## 已完成 ✅

### 1. 项目骨架
- [x] SpringBoot 2.7.18 项目初始化
- [x] 依赖管理：web, mybatis-plus, redis, jwt, knife4j, validation, bcrypt
- [x] 配置文件 `application.yml`（MySQL + Redis + JWT 密钥 + MP 驼峰映射）
- [x] 启动类 `HhwSeckillProjectApplication`

### 2. 数据库设计（4 张表）
| 表名 | 实体类 | 说明 |
|------|--------|------|
| `seckill_user` | `SeckillUser` | 用户表（手机号登录，BCrypt 密码） |
| `seckill` | `Seckill` | 秒杀商品表（库存、价格、时间窗口、乐观锁 version） |
| `seckill_order` | `SeckillOrder` | 秒杀订单表（用户、商品、价格、状态） |
| `user_seckill_record` | `UserSeckillRecord` | 用户秒杀记录表（唯一键防重） |

### 3. 基础架构层
- [x] **统一返回**：`Result<T>`（`common/Result.java`）
- [x] **常量类**：`ResultCodeConstant`、`ResultMessageConstant`
- [x] **全局异常处理**：`GlobalExceptionHandle` + `BusinessException`
- [x] **枚举**：`SeckillEnum`

### 4. 用户认证模块
- [x] **注册**：`RegisterController` → `RegisterService` → `RegisterServiceImpl`
- [x] **登录**：`LoginController` → `LoginService` → `LoginServiceImpl`
- [x] **JWT 工具类**：`JwtUtil`
- [x] **密码加密工具**：`PasswordEncoder`（BCrypt）
- [x] **JWT 拦截器**：`JwtInterceptor`（验证 token，存 userId 到 UserHolder）
- [x] **ThreadLocal 工具**：`UserHolder`（存取 userId，拦截器 afterCompletion 自动清理）
- [x] **拦截器配置**：`WebConfig`（拦截 `/hhw/**`，放行 `/hhw/login`、`/hhw/register`）

### 5. DTO 层
- [x] `LoginDto`：phone + password
- [x] `RegisterDto`：phone + nickname + password
- [x] `SeckillDto`：秒杀商品创建参数
- [x] `SeckillOrderDto`：秒杀订单参数

---

## 未完成 ❌

### 6. 核心秒杀业务（进行中 🚧）

**已就绪的基础设施：**
- [x] `@Version` 乐观锁注解（`Seckill.java`）
- [x] `MybatisplusConfig`：乐观锁拦截器 + 分页拦截器
- [x] `RedisConfig`：JSON 序列化 `RedisTemplate<String, Object>`
- [x] `SeckillController`：列表 `GET /seckills`、详情 `GET /{id}`
- [x] `SeckillService.getSeckillList()` 已实现
- [x] `SeckillService.onSeckill()` 方法签名已有

**待完成：**
- [ ] **Controller**：`onSeckill` 接口改成 `POST /{seckillId}`，通过 `UserHolder.getUserId()` 取用户 ID
- [ ] **Service 实现**：`onSeckill()` 核心逻辑（7 步）
  - [ ] 0. Redis 检查 `seckill:over:{id}` 秒杀是否已结束
  - [ ] 1. 查询商品
  - [ ] 2. 判断秒杀时间窗口
  - [ ] 3. 判断库存是否充足
  - [ ] 4. 防止用户重复秒杀（`user_seckill_record` 唯一键）
  - [ ] 5. 乐观锁扣减库存（`baseMapper.updateById`）
  - [ ] 6. 创建订单 `seckill_order`（状态 UNPAY=1000）
  - [ ] 7. 创建秒杀记录 `user_seckill_record`
- [ ] **WebConfig**：放行 `GET /hhw/seckill/seckills`、`GET /hhw/seckill/**` 查询接口

### 7. 待规划
- [ ] 订单支付
- [ ] 秒杀结果查询
- [ ] 接口限流 / 防刷

---

## 关键文件索引

```
src/main/java/com/gdou/
├── HhwSeckillProjectApplication.java    # 启动类
├── common/Result.java                   # 统一返回
├── Constant/
│   ├── ResultCodeConstant.java          # 状态码常量
│   └── ResultMessageConstant.java       # 消息常量
├── Enum/SeckillEnum.java                # 秒杀枚举
├── config/
│   ├── WebConfig.java                    # 拦截器配置
│   ├── MybatisplusConfig.java            # MP 乐观锁 + 分页插件
│   └── RedisConfig.java                  # Redis JSON 序列化配置
├── interceptor/JwtInterceptor.java      # JWT 拦截器
├── exception/
│   ├── BusinessException.java           # 业务异常
│   └── GlobalExceptionHandle.java       # 全局异常处理
├── util/
│   ├── JwtUtil.java                     # JWT 工具
│   ├── PasswordEncoder.java             # BCrypt 密码工具
│   └── UserHolder.java                  # ThreadLocal 存储当前用户ID
├── controller/
│   ├── RegisterController.java          # 注册接口 POST /hhw/register
│   ├── LoginController.java             # 登录接口 POST /hhw/login
│   └── SeckillController.java           # 秒杀接口 POST /hhw/seckill（空壳）
├── service/
│   ├── RegisterService.java / impl/
│   ├── LoginService.java / impl/
│   ├── SeckillUserService.java / impl/
│   ├── SeckillService.java / impl/       # 空壳，待实现
│   ├── SeckillOrderService.java / impl/
│   └── UserSeckillRecordService.java / impl/
├── mapper/（4 个 Mapper 接口）
├── pojo/
│   ├── entity/（SeckillUser, Seckill, SeckillOrder, UserSeckillRecord）
│   └── dto/（LoginDto, RegisterDto, SeckillDto, SeckillOrderDto）
└── resources/
    ├── application.yml
    └── sql/seckill.sql
```
