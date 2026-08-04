local key = KEYS[1]
local limit = tonumber(ARGV[1])
local time = tonumber(ARGV[2])
local count =redis.call("incr", key)
if count ==1 then
    redis.call("expire", key,time)
end
if count > limit then
    return -1
end
return 1
