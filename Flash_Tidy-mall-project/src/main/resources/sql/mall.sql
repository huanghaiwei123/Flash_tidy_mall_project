-- ============================================================
-- 潮汐商城（Flash Mall）数据库初始化脚本
-- 版本: v1.0
-- 日期: 2026-08-06
-- 说明: 9 张核心表，适配 SpringBoot 2.7.18 + MyBatis-Plus 3.5.5
-- ============================================================

CREATE DATABASE IF NOT EXISTS flash_mall
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE flash_mall;

-- ============================================================
-- 1. 用户表（替代旧 seckill_user，新增 gender / birthday / deleted）
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    `username`      VARCHAR(64)  NOT NULL                 COMMENT '用户名',
    `password`      VARCHAR(128) NOT NULL                 COMMENT '密码（BCrypt 加密）',
    `phone`         VARCHAR(20)  DEFAULT NULL             COMMENT '手机号',
    `email`         VARCHAR(128) DEFAULT NULL             COMMENT '邮箱',
    `nickname`      VARCHAR(64)  DEFAULT NULL             COMMENT '昵称',
    `avatar`        VARCHAR(256) DEFAULT NULL             COMMENT '头像 URL',
    `gender`        TINYINT      DEFAULT 0                COMMENT '性别：0=未知 1=男 2=女',
    `birthday`      DATE         DEFAULT NULL             COMMENT '生日',
    `status`        TINYINT      DEFAULT 1                COMMENT '账号状态：0=禁用 1=正常',
    `deleted`       TINYINT      DEFAULT 0                COMMENT '逻辑删除：0=正常 1=已删除',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone`    (`phone`),
    KEY `idx_create_time`    (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';


-- ============================================================
-- 2. 收货地址表（新增）
-- ============================================================
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '地址ID',
    `user_id`       BIGINT       NOT NULL                 COMMENT '用户ID',
    `receiver_name` VARCHAR(32)  NOT NULL                 COMMENT '收件人姓名',
    `phone`         VARCHAR(20)  NOT NULL                 COMMENT '收件人电话',
    `province`      VARCHAR(32)  NOT NULL                 COMMENT '省',
    `city`          VARCHAR(32)  NOT NULL                 COMMENT '市',
    `district`      VARCHAR(32)  DEFAULT NULL             COMMENT '区/县',
    `detail`        VARCHAR(256) NOT NULL                 COMMENT '详细地址',
    `is_default`    TINYINT      DEFAULT 0                COMMENT '是否默认：0=否 1=是',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id`   (`user_id`),
    KEY `idx_user_default` (`user_id`, `is_default`),
    unique key `idx_is_default_user_id` (`is_default`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';

-- ============================================================
-- 3. 商品分类表（树形结构，新增）
-- ============================================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '分类ID',
    `parent_id`     BIGINT       DEFAULT 0                COMMENT '父分类ID，0 表示一级分类',
    `name`          VARCHAR(64)  NOT NULL                 COMMENT '分类名称',
    `icon`          VARCHAR(256) DEFAULT NULL             COMMENT '图标 URL',
    `level`         TINYINT      DEFAULT 1                COMMENT '层级：1/2/3',
    `sort`          INT          DEFAULT 0                COMMENT '排序值（越小越靠前）',
    `is_leaf`       TINYINT      DEFAULT 1                COMMENT '是否叶子节点：0=否 1=是',
    `status`        TINYINT      DEFAULT 1                COMMENT '状态：0=隐藏 1=显示',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort`      (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表（树形）';


-- ============================================================
-- 4. SPU 商品主表（新增）
-- ============================================================
DROP TABLE IF EXISTS `spu`;
CREATE TABLE `spu` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT 'SPU ID',
    `name`          VARCHAR(128) NOT NULL                 COMMENT '商品名称',
    `description`   VARCHAR(512) DEFAULT NULL             COMMENT '商品描述',
    `category_id`   BIGINT       DEFAULT 0                COMMENT '所属分类ID',
     category_name  varchar(64)  not null                 comment '所属分类名称',
     merchant_id    bigint       not null                 comment '商家id',
    `brand`         VARCHAR(64)  DEFAULT NULL             COMMENT '品牌',
    `main_image`    VARCHAR(256) DEFAULT NULL             COMMENT '主图 URL',
    `images`        JSON         DEFAULT NULL             COMMENT '商品轮播图（JSON 数组）',
    `detail`        LONGTEXT     DEFAULT NULL             COMMENT '商品详情（富文本 HTML）',
    `sales`         INT          DEFAULT 0                COMMENT '销量',
    `status`        TINYINT      DEFAULT 1                COMMENT '状态：0=下架 1=上架',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id_name` (`category_id`,`name`),
    KEY `idx_status`      (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SPU 主表';
-- ============================================================
-- 5. SKU 库存单元表（替代旧 seckill 表，含秒杀字段 + 库存三字段）
-- ============================================================
DROP TABLE IF EXISTS `sku`;
CREATE TABLE `sku` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT 'SKU ID',
    `spu_id`            BIGINT        NOT NULL                 COMMENT '所属 SPU ID',
    `name`              VARCHAR(256)  NOT NULL                 COMMENT 'SKU 名称（规格组合，如 "iPhone 15 黑色 256G"）',
    `spec`              VARCHAR(256)  DEFAULT NULL             COMMENT '规格描述（JSON，如 [{"k":"颜色","v":"黑色"},{"k":"容量","v":"256G"}]）',
    `price`             DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '售价',
    `original_price`    DECIMAL(10,2) DEFAULT NULL             COMMENT '原价/划线价',
    `image`             VARCHAR(256)  DEFAULT NULL             COMMENT 'SKU 图片 URL',

    -- 库存三字段（见 PROGRESS.md）
    `stock`             INT           NOT NULL DEFAULT 0       COMMENT '总库存（商家设置）',
    `locked_stock`      INT           NOT NULL DEFAULT 0       COMMENT '锁定库存（下单未付）',
    `available_stock`   INT           NOT NULL DEFAULT 0       COMMENT '可售库存（= stock - locked_stock）',
    `promotion_stock`   INT           NOT NULL DEFAULT 0       COMMENT '促销/秒杀库存配额（≤ stock）',

    -- 秒杀相关字段
    `is_seckill`        TINYINT       DEFAULT 0                COMMENT '是否参与秒杀：0=否 1=是',
    `is_seckill_stock`   int          not null default 0       comment '参与秒杀商品库存',
    `seckill_price`     DECIMAL(10,2) DEFAULT NULL             COMMENT '秒杀价格',
    `seckill_start_time` DATETIME     DEFAULT NULL             COMMENT '秒杀开始时间',
    `seckill_end_time`  DATETIME      DEFAULT NULL             COMMENT '秒杀结束时间',

    `sort`              INT           DEFAULT 0                COMMENT '排序值（越小越靠前）',
    `status`            TINYINT       DEFAULT 1                COMMENT '状态：0=下架 1=上架',
    `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id`        (`spu_id`),
    KEY `idx_is_seckill`    (`is_seckill`),
    KEY `idx_seckill_time`  (`is_seckill`, `seckill_start_time`, `seckill_end_time`),
    KEY `idx_status`        (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU 库存单元表（含秒杀 + 库存三字段）';

-- ============================================================
-- 6. 统一订单表（替代旧 seckill_order + user_seckill_record，order_type 区分普通/秒杀）
-- ============================================================
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '订单ID',
    `order_no`          VARCHAR(36)   NOT NULL                 COMMENT '订单编号（唯一，业务流水号）',
    `user_id`           BIGINT        NOT NULL                 COMMENT '用户ID',
    -- 订单类型 & 状态
    `order_type`        VARCHAR(16)   NOT NULL DEFAULT 'NORMAL' COMMENT '订单类型：NORMAL=普通 SECKILL=秒杀',
    `status`            VARCHAR(24)   NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态：PENDING_PAY=待支付 PAID=已支付 SHIPPED=已发货 RECEIVED=已收货 COMPLETED=已完成 CANCELLED=已取消 REFUNDING=退款中 REFUNDED=已退款',

    -- 金额（单位：元）
    `total_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '商品总金额',
    `discount_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '优惠券减免金额',
    `pay_amount`        DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '实付金额（= total_amount - discount_amount）',
    `user_coupon_id`    BIGINT        DEFAULT NULL             COMMENT '使用的用户优惠券ID',

    -- 收货地址快照
    `address_snapshot`  JSON          DEFAULT NULL             COMMENT '收货地址快照（下单时冗余存储，不受地址变更影响）',

    -- 支付信息
    `pay_time`          DATETIME      DEFAULT NULL             COMMENT '支付时间',
    `pay_type`          VARCHAR(16)   DEFAULT 'ALIPAY'         COMMENT '支付方式：ALIPAY=支付宝',
    `trade_no`          VARCHAR(64)   DEFAULT NULL             COMMENT '支付宝交易号',

    -- 时间
    `cancel_time`       DATETIME      DEFAULT NULL             COMMENT '取消时间',
    `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（下单时间）',
    `update_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no`       (`order_no`),
    KEY `idx_user_id`              (`user_id`),
    KEY `idx_user_order_type`      (`user_id`, `order_type`),
    KEY `idx_status`               (`status`),
    KEY `idx_create_time`          (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一订单表';

-- ============================================================
-- 7. 角色表 + 用户角色关联表（新增，RBAC 多对多）
-- ============================================================
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '角色ID',
    `code`          VARCHAR(32)  NOT NULL                 COMMENT '角色编码（如 ADMIN / MERCHANT / USER，鉴权时用）',
    `name`          VARCHAR(32)  NOT NULL                 COMMENT '角色名称（展示用）',
    `description`   VARCHAR(128) DEFAULT NULL             COMMENT '角色描述',
    `sort`          INT          DEFAULT 0                COMMENT '排序值',
    `status`        TINYINT      DEFAULT 1                COMMENT '状态：0=禁用 1=正常',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 初始角色数据
INSERT INTO `role` (`code`, `name`, `description`, `sort`) VALUES
('ADMIN',    '管理员', '拥有全部权限', 1),
('MERCHANT', '商家',   '管理自己的商品和订单', 2),
('USER',     '普通用户', '浏览和购买商品', 3);

DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT  COMMENT '关联ID',
    `user_id`       BIGINT   NOT NULL                 COMMENT '用户ID',
    `role_id`       BIGINT   NOT NULL                 COMMENT '角色ID',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表（多对多）';


-- ============================================================
-- 8. 订单明细表（新增）
-- ============================================================
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '明细ID',
    `order_id`      BIGINT        NOT NULL                 COMMENT '订单ID',
    `order_no`      VARCHAR(36)   NOT NULL                 COMMENT '订单编号（冗余，方便查询）',
    `spu_id`        BIGINT        NOT NULL                 COMMENT 'SPU ID',
    `sku_id`        BIGINT        NOT NULL                 COMMENT 'SKU ID',
    `merchant_id`   BIGINT        NOT NULL                 COMMENT '商家ID（冗余，方便商家查询属于自己的订单明细）',

    -- SKU 快照（下单时冗余存储，不受商品变更影响）
    `sku_name`      VARCHAR(256)  NOT NULL                 COMMENT 'SKU 名称快照',
    `sku_spec`      VARCHAR(256)  DEFAULT NULL             COMMENT 'SKU 规格快照',
    `sku_image`     VARCHAR(256)  DEFAULT NULL             COMMENT 'SKU 图片快照',
    `sku_price`     DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT 'SKU 下单时单价快照',

    `quantity`      INT           NOT NULL DEFAULT 1       COMMENT '购买数量',
    `total_price`   DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '明细总价（= sku_price × quantity）',
    `item_status`   VARCHAR(24)   NOT NULL DEFAULT 'PENDING' COMMENT '明细状态：PENDING=待处理 SHIPPED=已发货 RECEIVED=已收货',

    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id`  (`order_id`),
    KEY `idx_order_no`  (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';
-- ============================================================
-- 9. 优惠券模板表（管理员创建）
-- ============================================================
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '优惠券ID',
    `name`            VARCHAR(128)  NOT NULL                 COMMENT '优惠券名称',
    `type`            VARCHAR(16)   NOT NULL                 COMMENT '类型：FULL_REDUCTION=满减 DISCOUNT=折扣',
    `discount_value`  DECIMAL(10,2) NOT NULL                 COMMENT '优惠值（满减=减多少元；折扣=0.85即85折）',
    `min_amount`      DECIMAL(10,2) DEFAULT 0.00            COMMENT '最低消费金额（0=无门槛）',
    `total_count`     INT           DEFAULT 0                COMMENT '发放总量',
    `issued_count`    INT           DEFAULT 0                COMMENT '已领取数量',
    `per_user_limit`  INT           DEFAULT 1                COMMENT '每人限领数量',
    `start_time`      DATETIME      NOT NULL                 COMMENT '有效期开始',
    `end_time`        DATETIME      NOT NULL                 COMMENT '有效期结束',
    `status`          TINYINT       DEFAULT 1                COMMENT '状态：0=禁用 1=正常',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status`      (`status`),
    KEY `idx_time`        (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表（管理员创建）';


-- ============================================================
-- 10. 优惠券-商家关联表（商家选择参与哪些优惠券）
-- ============================================================
DROP TABLE IF EXISTS `coupon_merchant`;
CREATE TABLE `coupon_merchant` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT  COMMENT '关联ID',
    `coupon_id`     BIGINT   NOT NULL                 COMMENT '优惠券ID',
    `merchant_id`   BIGINT   NOT NULL                 COMMENT '商家用户ID',
    `status`        TINYINT  DEFAULT 1                COMMENT '是否参与：0=不参与 1=参与',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_merchant` (`coupon_id`, `merchant_id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券-商家关联表';


-- ============================================================
-- 11. 用户优惠券表（用户领取/使用的优惠券）
-- ============================================================
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT  COMMENT '记录ID',
    `user_id`       BIGINT        NOT NULL                 COMMENT '用户ID',
    `coupon_id`     BIGINT        NOT NULL                 COMMENT '优惠券ID',
    `status`        VARCHAR(16)   DEFAULT 'UNUSED'         COMMENT '状态：UNUSED=未使用 USED=已使用 EXPIRED=已过期',
    `order_id`      BIGINT        DEFAULT NULL             COMMENT '使用的订单ID',
    `used_time`     DATETIME      DEFAULT NULL             COMMENT '使用时间',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id`       (`user_id`),
    KEY `idx_user_coupon`   (`user_id`, `coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';


-- ============================================================
-- 12. 购物车表
-- ============================================================
DROP TABLE IF EXISTS `shopping_car`;
CREATE TABLE `shopping_car` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT  COMMENT '购物车记录ID',
    `user_id`       BIGINT   NOT NULL                 COMMENT '用户ID',
    `spu_id`        BIGINT   NOT NULL                 COMMENT 'SPU ID（冗余，方便展示商品名、图片）',
    `sku_id`        BIGINT   NOT NULL                 COMMENT 'SKU ID',
    `merchant_id`   BIGINT       NOT NULL                 COMMENT '商家ID',
    `quantity`      INT      NOT NULL DEFAULT 1       COMMENT '购买数量',
    `selected`      TINYINT  DEFAULT 1                COMMENT '是否勾选：0=未勾选 1=勾选',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ============================================================
-- 商家申请表 — 用户申请成为商家，管理员审核
-- ============================================================
DROP TABLE IF EXISTS `merchant_application`;
CREATE TABLE `merchant_application` (
                                        `id`                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '申请ID',
                                        `user_id`           BIGINT       NOT NULL                 COMMENT '申请人用户ID',
                                        `shop_name`         VARCHAR(64)  NOT NULL                 COMMENT '店铺名称',
                                        `shop_description`  VARCHAR(255) DEFAULT NULL             COMMENT '店铺简介',
                                        `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING=待审核 APPROVED=已通过 REJECTED=已拒绝',
                                        `review_comment`    VARCHAR(255) DEFAULT NULL             COMMENT '审核备注',
                                        `reviewer_id`       BIGINT       DEFAULT NULL             COMMENT '审核人ID（管理员）',
                                        `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_user_id`   (`user_id`),
                                        KEY `idx_status`    (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家申请表';

-- ============================================================
-- 13. 商家表（店铺信息，id 与 user.id 一致——商家即用户）
-- ============================================================
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant` (
    `id`                BIGINT       NOT NULL                  COMMENT '商家ID（= 用户ID，与 user.id 一致）',
    `shop_name`         VARCHAR(64)  NOT NULL                 COMMENT '店铺名称',
    `shop_logo`         VARCHAR(256) DEFAULT NULL             COMMENT '店铺 Logo URL',
    `shop_description`  VARCHAR(512) DEFAULT NULL             COMMENT '店铺简介',
    `contact_phone`     VARCHAR(20)  DEFAULT NULL             COMMENT '店铺联系电话',
    `status`            TINYINT      DEFAULT 1                COMMENT '状态：0=禁用 1=正常',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_name` (`shop_name`),
    KEY `idx_status`    (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表（店铺信息，id与user一致）';

