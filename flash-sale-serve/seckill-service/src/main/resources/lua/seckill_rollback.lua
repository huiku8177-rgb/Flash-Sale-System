-- KEYS[1] = 库存 key
-- KEYS[2] = 用户抢购记录 key
-- ARGV[1] = userId

local stockKey = KEYS[1]
local userKey = KEYS[2]
local userId = ARGV[1]

-- 只有用户在已抢购集合中，才回滚库存
if redis.call("SISMEMBER", userKey, userId) == 1 then
    redis.call("SREM", userKey, userId)
    redis.call("INCR", stockKey)
    return 1
end

return 0
