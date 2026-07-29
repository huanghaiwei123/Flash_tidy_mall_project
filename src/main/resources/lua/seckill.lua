local userSeckillKey = KEYS[1]
local stockKey = KEYS[2]

--检查是否已被秒杀过
if redis.call("exists", userSeckillKey) == 1 then
    return -1
end

--扣减库存
local stock = redis.call("get", stockKey)
if not stock or tonumber(stock) <= 0 then
    return 0
end

--扣库存，标记用户已秒杀
redis.call("decr", stockKey)
redis.call("set", userSeckillKey, '1')
return 1