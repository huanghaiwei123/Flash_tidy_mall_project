local stockKey = KEYS[1]
local startTimeKey = KEYS[2]
local endTimeKey = KEYS[3]
local userKey = KEYS[4]

-- 判断是否有这个秒杀商品
if redis.call("exists", stockKey) == 0 then
    return -1;
end

-- 判断活动时间
local now = tonumber(redis.call("TIME")[1]);
local startTime = tonumber(redis.call("get", startTimeKey))
local endTime = tonumber(redis.call("get", endTimeKey))
if now < startTime then
    return -2
end
if now > endTime then
    return -3
end

-- 判断是否已经秒杀
if not redis.call("set", userKey, 1, "NX", "EX", 24 * 60 * 60) then
    return -4
end

-- 商品库存大于0减一
local stock = tonumber(redis.call("decrby", stockKey, 1));
if stock < 0 then
    redis.call("incr", stockKey);
    redis.call("del", userKey)
    return -5
end
return 1;
