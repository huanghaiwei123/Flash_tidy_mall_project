-- KEYS[1]:  用户秒杀记录 key
-- KEYS[2] ~ KEYS[11]:  10 个库存桶 key（bucket 0 ~ bucket 9）
-- KEYS[12]: 活动开始时间 key（epoch 毫秒）
-- KEYS[13]: 活动结束时间 key（epoch 毫秒）
-- ARGV[1]: 当前时间（epoch 毫秒）
-- 返回值: -4=未预热, -3=已结束, -2=未开始, -1=已参与, 0=卖完, 1~10=成功(桶编号 1-based)

-- ① 时间窗口校验（从 Redis 读取，与扣库存同一次网络往返）
local startTime = tonumber(redis.call("get", KEYS[12]))
local endTime = tonumber(redis.call("get", KEYS[13]))
local now = tonumber(ARGV[1])
if not startTime or not endTime then
    return -4
end
if now < startTime then
    return -2
end
if now > endTime then
    return -3
end

-- ② 检查是否已秒杀过
local userSeckillKey = KEYS[1]
if redis.call("exists", userSeckillKey) == 1 then
    return -1
end

-- ③ 10 个桶下标装进数组并洗牌
local buckets = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}
math.randomseed(tonumber(redis.call('TIME')[2]))

-- Fisher-Yates 洗牌
for i = 10, 2, -1 do
    local j = math.random(i)
    buckets[i], buckets[j] = buckets[j], buckets[i]
end

-- ④ 按乱序尝试每个桶
for i, bucketIdx in ipairs(buckets) do
    local stockKey = KEYS[bucketIdx + 2]
    local stock = redis.call("get", stockKey)
    if stock and tonumber(stock) > 0 then
        redis.call("decr", stockKey)
        redis.call("set", userSeckillKey, "1", "EX", "3600")
        return bucketIdx + 1
    end
end

-- ⑤ 所有桶都没库存
return 0
