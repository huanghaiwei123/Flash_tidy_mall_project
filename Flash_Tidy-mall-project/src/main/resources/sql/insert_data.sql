INSERT INTO `merchant` (`id`, `shop_name`, `shop_logo`, `shop_description`, `contact_phone`, `status`)
VALUES (2, '潮汐优选旗舰店', 'http://localhost:8080/img/logo.png', '潮汐商城官方自营店铺，品质保证', '13800138000', 1);

SELECT id, spu_id, name, is_seckill, seckill_price, seckill_start_time, seckill_end_time
FROM sku
WHERE is_seckill = 1 AND status = 1;
