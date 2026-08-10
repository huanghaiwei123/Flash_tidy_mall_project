# 潮汐商城（Flash Mall）开发进度

> 最后更新：2026-08-14

## 项目概况

| 项 | 值 |
|------|------|
| **项目名** | `hhw-mall-project`（潮汐商城） |
| **主模块** | `Flash_Tidy-mall-project`（`flash-mall`） |
| **技术栈** | JDK 8 / SpringBoot 2.7.18 / MyBatis-Plus 3.5.5 / Redis (Lettuce) / RabbitMQ / JWT / BCrypt / Guava / 支付宝沙箱 / Knife4j |
| **包路径** | `com.gdou` |
| **接口前缀** | `/hhw` |
| **当前状态** | 🟢 全功能已落地：用户/商家/管理员三端 + 支付 + 秒杀，等编译验证 |

---

## 2026-08-14 全量功能盘点（对照代码）

> 本次盘点对照源码确认实际完成情况，修正了此前 PROGRESS.md 中"支付逻辑待办、地址缺 Controller"等已过时结论。

### 用户端 ✅
- 注册 / 登录（JWT + BCrypt）— `LoginController` / `RegisterController`
- 商品浏览：列表（分类/关键词/分页）、详情、SKU 列表、分类树、店铺主页 — `GoodsController`
- 收货地址 CRUD — `UserAddressController`
- 购物车：增删改查/改数量/勾选/全选/汇总 — `UserShoppingCarController`
- 下单：普通下单、查单、订单列表、取消、确认收货、再来一单 — `UserOrderController`
- 优惠券：可领列表/领取/我的券（状态筛选）/下单验券 — `UserCouponController`
- 秒杀：查秒杀商品、算术验证码、秒杀下单 — `UserSeckillController`
- 个人中心：改资料、商家申请、切换卖家/管理员页 — `UserController`

### 商家端 ✅
- SPU：查询/上架/更新/上下架 — `MerchantSpuController`
- SKU：增删改查/上下架/跨 SPU 汇总 — `MerchantSkuController`
- 订单：查单/店铺订单列表/发货 — `MerchantOrderController`
- 秒杀活动列表（含实时库存）— `MerchantSeckillController`
- 优惠券 opt-in/opt-out — `MerchantCouponController`

### 管理员端 ✅
- 商家申请审核（筛选 + 通过/拒绝）— `admin/MerchantController`
- 优惠券管理（增改查）— `admin/CouponController`
- 分类管理（增删改查 + 树）— `admin/CategoryController`

### 支付模块 ✅（支付宝沙箱完整接入）
- `PayServiceImpl`：页面支付、异步通知 RSA2 验签 + app_id + 金额校验、同步回调主动查单兜底、退款（支付宝先退 + 本地乐观锁 + 恢复库存）
- 订单状态流转：待支付 → 已支付 → 已发货 → 已收货 / 已取消 / 已退款

### 秒杀模块 ✅
- Redis Lua 原子扣减（`seckill.lua`）+ 算术验证码防脚本（一次性）+ RabbitMQ 异步建单 + 消息幂等（`MqOrderMessage` messageId）
- 定时任务：`SeckillTask` / `EsSyncTask`
- 压测：TPS 天花板 ~1500–1700（见 `seckill-performance-test-2026-08-12.md`）

### 前端页面（static，12 个 HTML）✅
`index` / `shop` / `goods-detail` / `cart` / `order-list` / `seckill` / `address` / `user-center` / `login` / `register` / `merchant` / `admin`

### 遗留事项
- [ ] 编译验证（JDK 8 工具链；本机 JDK 23 可能编译失败）
- [ ] `pom.xml` 缺少 commons-pool2 依赖 → Lettuce 连接池未生效（压测文档已记录）

---

## 2026-08-09 开发日志

### 前端修复
- Logo 显示修复（超大原图撑爆布局 → CSS 限制 + 图片缩至 135×42）
- 去除 AI 水印（裁剪底部 90px）
- 分类栏清理（双边框 → 单边框，去掉 `☰` 改 `▾`）
- 登录/注册页去掉顶部导航栏
- Toast warning 样式补全（橙色背景，非管理员提示可见）

### 管理员功能
- 新增 `POST /hhw/user/changeAdminPage` 接口（角色验证）
- 导航栏加「⚙️ 管理」入口，非管理员 toast 拦截
- 管理后台新增「商家申请审核」Tab（筛选 + 通过/拒绝）
- 分类树改为折叠箭头 + 颜色分层 + 去掉图标列

### 下单地址选择
- cart.html 不再用 prompt()，加载地址列表卡片
- 默认地址自动选中（蓝框 + 「默认」标签）
- 下单时地址写入 addressSnapshot

### 待办（明日）
- [x] 支付逻辑（支付宝沙箱适配）→ 已于后续完成，见 2026-08-14 盘点

---

## 2026-08-07 开发日志

### 今天完成的工作

**1. 清理 Gradle 残留**
- 删除 `.idea/gradle.xml`（引用了不存在的 Gradle 项目，导致 IDE 持续报构建错误）
- 清理 `.idea/misc.xml` 的 `ExternalStorageConfigurationManager`
- 清理 `.gitignore` 中 Gradle 相关条目

**2. 修改记录**
- `@ComponentScan("com.gdou.mapper")` 覆盖了 SpringBoot 默认扫描 → 导致 WebConfig/JwtUtil 自动注入失败 → 待改为 `@MapperScan`
- `spu` 表新增 `merchant_id` 字段，建立商品与商家关联
- `order` 表新增 `discount_amount`（优惠减免）、`user_coupon_id`（使用的券）、`merchant_id`、`spu_name`、`category_name`
- `OrderDto.merchantId` 类型从 `String` 修正为 `Long`

**3. 新增优惠券模块**（3 张表 + 完整三层 + admin/merchant/user 三个 Controller）
- `coupon` — 优惠券模板表（管理员创建，满减/折扣两种类型）
- `coupon_merchant` — 商家参与关联表（商家选择哪些券可用）
- `user_coupon` — 用户领券记录表（领取/使用/过期）
- 接口：管理员 CRUD、商家 opt-in/opt-out、用户领券/查券/验证券

**4. 新增购物车模块**（1 张表 + 完整三层）
- `shopping_car` — 用户-商品-SKU 关联，`uk_user_sku` 防重复，`selected` 字段支持勾选
- 接口：添加、列表（含 SKU 详情+小计）、汇总（只算勾选的）、改数量、切换勾选、全选、删除

**5. DTO 校验补全**
- 全部 7 个 DTO 加了 `javax.validation` 注解（`@NotBlank`/`@NotNull`/`@Pattern`/`@Size`/`@Email`/`@DecimalMin`/`@Min`）
- 创建 `GlobalExceptionHandler` — 校验失败返回中文提示而非 500
- 创建 `BusinessException` — 业务异常统一处理
- 全部 6 个 `@RequestBody` Controller 方法加上了 `@Valid`

**6. 全面 CRUD 审计**
- 13 张表的 CRUD 覆盖率已整理完毕
- 发现 6 个严重 Bug（5 个已修复：登录 BCrypt 验证反了、取消订单改了错误字段、JWT 验证逻辑反了、角色查询用错 ID、RegisterServiceImpl 错误 import）
- 缺失优先级：收货地址 CRUD > 下单接口 > 商品浏览 > 商家 SKU 管理 > 分类管理

### 待办（明日）

- [ ] 修复 `@ComponentScan` → `@MapperScan`
- [ ] 收货地址 CRUD（下单前置依赖）
- [ ] 用户下单接口（从购物车结算创建订单）
- [ ] 商品浏览接口（用户端 SPU 列表/详情）
- [ ] 商家 SKU 管理
- [ ] 编译验证

---

## 数据库设计

### 表结构（共 13 张）

| 表 | 说明 | 状态 |
|------|------|:---:|
| `user` | 用户表 | 🟢 |
| `user_address` | 收货地址 | 🟢 |
| `category` | 商品分类（树形） | 🟢 |
| `spu` | 商品主表（+ merchant_id） | 🟢 |
| `sku` | 库存单元（含秒杀 + 库存三字段） | 🟢 |
| `role` | 角色表（RBAC） | 🟢 随用户模块使用 |
| `user_role` | 用户角色关联 | 🟢 随用户模块使用 |
| `order` | 统一订单 | 🟢 |
| `order_item` | 订单明细 | 🟢 随 order 创建 |
| `coupon` | 优惠券模板 | 🟢 |
| `coupon_merchant` | 优惠券-商家关联 | 🟢 |
| `user_coupon` | 用户优惠券 | 🟢 |
| `shopping_car` | 购物车 | 🟢 |

### 库存三字段（sku 表）

| 字段 | 含义 | 关系 |
|------|------|------|
| `stock` | 总库存 | 商家设置 |
| `locked_stock` | 锁定库存（下单未付） | 下单 +1，支付/取消 -1 |
| `available_stock` | 可售库存 | = stock - locked_stock |
| `promotion_stock` | 促销库存（秒杀配额） | ≤ stock，秒杀时扣减 |

## 核心域开发计划

- [x] **基础架构**：Result、异常处理、常量、MP 配置、Redis 配置
- [x] **用户模块**：注册 / 登录（JWT）/ 收货地址
- [x] **商品模块**：分类 CRUD / SPU CRUD / SKU CRUD + 库存管理
- [x] **订单模块**：普通下单 / 订单列表 / 订单状态流转
- [x] **支付模块**：支付宝沙箱（适配新订单表）
- [x] **秒杀模块**：is_seckill + promotion_stock + Lua 原子扣减 + MQ 异步 + 布隆过滤器 + 验证码
- [x] **购物车**：已改为数据库存储（非 Redis hash），含勾选、全选、汇总
- [x] **优惠券模块**：管理员发券、商家参与、用户领券/验券

## 接口规划（实际已实现）

| 方法 | 路径 | 说明 | 登录 |
|:---:|------|------|:---:|
| POST | `/hhw/register` | 注册 | ❌ |
| POST | `/hhw/login` | 登录 | ❌ |
| GET | `/hhw/goods/list` | 商品列表 | ❌ |
| GET | `/hhw/goods/{spuId}` | 商品详情 | ❌ |
| GET | `/hhw/goods/{spuId}/skus` | SKU 列表 | ❌ |
| GET | `/hhw/goods/categories` | 分类树 | ❌ |
| GET | `/hhw/goods/shop/{merchantId}` | 店铺主页 | ❌ |
| POST | `/hhw/user/order/create` | 创建订单（NORMAL/SECKILL） | ✅ |
| GET | `/hhw/user/order/query` | 订单详情 | ✅ |
| GET | `/hhw/user/order/queryList` | 我的订单 | ✅ |
| POST | `/hhw/user/order/delete/{orderNo}` | 取消订单 | ✅ |
| POST | `/hhw/user/order/receive/{orderNo}` | 确认收货 | ✅ |
| POST | `/hhw/user/order/reorder/{orderNo}` | 再来一单 | ✅ |
| POST | `/hhw/seckill/start` | 执行秒杀 ⚡ | ✅ |
| GET | `/hhw/seckill/query` | 秒杀商品列表 | ✅ |
| GET | `/hhw/seckill/captcha` | 秒杀验证码 | ✅ |
| POST | `/hhw/pay/{orderNo}` | 支付宝支付 | ✅ |
| POST | `/hhw/pay/refund/{orderNo}` | 退款 | ✅ |
| POST | `/hhw/pay/notify` | 支付宝异步回调 | ❌ RSA2 |
| GET | `/hhw/pay/return` | 支付宝同步回调 | ❌ |
| POST/GET/PUT/DELETE | `/hhw/address/**` | 收货地址 CRUD | ✅ |
| POST/GET/PUT/DELETE | `/hhw/user/cart/**` | 购物车 | ✅ |
| GET/POST | `/hhw/user/coupon/**` | 用户优惠券 | ✅ |
| POST/GET | `/hhw/merchant/**` | 商家：SPU/SKU/订单/发货/秒杀/券 | ✅ |
| GET/POST/PUT/DELETE | `/hhw/admin/**` | 管理员：审核/优惠券/分类 | ✅ |

## 历史记录

~~旧版秒杀原型（2026-07-26 ~ 2026-08-06）：4 张表、Redis Cluster 分桶、MQ 异步、支付宝沙箱、压测 1131 QPS。详见 git 历史 `7e27370` 及之前的提交。2026-08-06 删除 seckill-app 模块，从头重建。~~
