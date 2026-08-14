-- 秒杀活动演示数据（2026-08-12 14:00 ~ 16:00）
-- 执行前先确认 SKU ID 是否存在：SELECT id, spu_id, name, price FROM sku WHERE status = 1;

-- 修改以下 SKU ID 为你库里实际存在的 ID
UPDATE `sku`
SET is_seckill          = 1,
    seckill_price       = 9.90,       -- 秒杀价
    promotion_stock     = 100,        -- 秒杀配额
    seckill_start_time  = '2026-08-12 14:00:00',
    seckill_end_time    = '2026-08-12 16:00:00',
    status              = 1
WHERE id =23;

UPDATE `sku`
SET is_seckill          = 1,
    seckill_price       = 19.90,
    promotion_stock     = 80,
    seckill_start_time  = '2026-08-12 14:00:00',
    seckill_end_time    = '2026-08-12 16:00:00',
    status              = 1
WHERE id = 26;

UPDATE `sku`
SET is_seckill          = 1,
    seckill_price       = 29.90,
    promotion_stock     = 60,
    seckill_start_time  = '2026-08-12 14:00:00',
    seckill_end_time    = '2026-08-12 16:00:00',
    status              = 1
WHERE id = 33;

UPDATE `sku`
SET is_seckill          = 1,
    seckill_price       = 39.90,
    promotion_stock     = 50,
    seckill_start_time  = '2026-08-12 14:00:00',
    seckill_end_time    = '2026-08-12 16:00:00',
    status              = 1
WHERE id = 35;

UPDATE `sku`
SET is_seckill          = 1,
    seckill_price       = 49.90,
    promotion_stock     = 30,
    seckill_start_time  = '2026-08-12 14:00:00',
    seckill_end_time    = '2026-08-12 16:00:00',
    status              = 1
WHERE id = 37;

SELECT
    MIN(create_time) AS 首单时间,
    MAX(create_time) AS 末单时间,
    COUNT(*)         AS 新增订单,
    ROUND(COUNT(*) / GREATEST(TIMESTAMPDIFF(SECOND, MIN(create_time), MAX(create_time)), 1), 1) AS 落库TPS
FROM `order`
WHERE order_type = 'SECKILL'
  AND create_time >= '2026-08-12 20:50:00';

SELECT MAX(id) FROM `order`;
SELECT MAX(id) FROM sku;




