/*
SQLyog 企业版 - MySQL GUI v8.14
MySQL - 5.7.17-log : Database - seckill
*********************************************************************
*/


/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- =============================================
-- 1. 秒杀用户表
-- =============================================
DROP TABLE IF EXISTS `seckill_user`;

CREATE TABLE `seckill_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `phone` varchar(20) NOT NULL COMMENT '手机号（登录账号）',
  `nickname` varchar(50) NOT NULL COMMENT '昵称',
  `password` varchar(200) NOT NULL COMMENT 'Bcrypt加密密码',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头`nickname` varchar(50) NOT NULL COMMENT ''昵称''像地址',
  `register_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT='秒杀用户表';
-- =============================================
-- 2. 秒杀商品表（增强版：增加图片、价格字段）
-- =============================================
DROP TABLE IF EXISTS `seckill`;

CREATE TABLE `seckill` (
  `seckill_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '商品库存id',
  `name` varchar(120) NOT NULL COMMENT '商品名称',
  `number` int(11) NOT NULL COMMENT '库存数量',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '秒杀价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `goods_img` varchar(200) DEFAULT NULL COMMENT '商品图片',
  `start_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '秒杀开启时间',
  `end_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '秒杀结束时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `version` int(11) NOT NULL COMMENT '版本号（乐观锁）',
  PRIMARY KEY (`seckill_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1004 DEFAULT CHARSET=utf8 COMMENT='秒杀库存表';

/*Data for the table `seckill` */

INSERT INTO `seckill`(`seckill_id`,`name`,`number`,`price`,`original_price`,`goods_img`,`start_time`,`end_time`,`create_time`,`version`) VALUES
(1000,'1000元秒杀iphone8',100,1000.00,5999.00,NULL,'2026-08-01 00:00:00','2026-12-31 23:59:59','2026-07-26 00:00:00',0),
(1001,'500元秒杀ipad2',100,500.00,2999.00,NULL,'2026-08-01 00:00:00','2026-12-31 23:59:59','2026-07-26 00:00:00',0),
(1002,'300元秒杀小米4',100,300.00,1499.00,NULL,'2026-08-01 00:00:00','2026-12-31 23:59:59','2026-07-26 00:00:00',0),
(1003,'200元秒杀红米note',100,200.00,899.00,NULL,'2026-08-01 00:00:00','2026-12-31 23:59:59','2026-07-26 00:00:00',0);

-- =============================================
-- 3. 秒杀订单表
-- =============================================
DROP TABLE IF EXISTS `seckill_order`;

CREATE TABLE `seckill_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `seckill_id` bigint(20) NOT NULL COMMENT '秒杀商品ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `goods_name` varchar(120) NOT NULL COMMENT '商品名称（冗余）',
  `price` decimal(10,2) NOT NULL COMMENT '秒杀价格',
  `state` int(4) NOT NULL DEFAULT '0' COMMENT '订单状态：0-未支付 1-已支付 2-已取消 3-已退款',
  `bucket_id` int(4) DEFAULT NULL COMMENT '命中的库存桶编号（0~9），用于精确回滚',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
  `trade_no` VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号',
  PRIMARY KEY (`order_id`),
  KEY `idx_seckill_id` (`seckill_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀订单表';
ALTER TABLE seckill_order ADD COLUMN bucket_id INT(4) DEFAULT NULL COMMENT '命中的库存桶编号（0~9），用于精确回滚';

-- =============================================
-- 4. 用户秒杀记录表（代替原来的 success_killed）
-- =============================================
DROP TABLE IF EXISTS `success_killed`;

CREATE TABLE `user_seckill_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `seckill_id` bigint(20) NOT NULL COMMENT '秒杀商品ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID',
  `state` int(4) NOT NULL DEFAULT '0' COMMENT '秒杀状态：0-秒杀成功待支付 1-支付成功 -1-秒杀失败',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seckill_user` (`seckill_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户秒杀记录表';

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- 开启慢日志（重启失效）
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 0.1;  -- 100ms 就算慢
SET GLOBAL log_queries_not_using_indexes = ON;
