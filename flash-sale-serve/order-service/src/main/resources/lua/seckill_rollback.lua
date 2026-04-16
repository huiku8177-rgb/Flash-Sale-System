-- KEYS[1] = stock key
-- KEYS[2] = user reservation key
-- ARGV[1] = userId

local stockKey = KEYS[1]
local userKey = KEYS[2]
local userId = ARGV[1]

if redis.call("SISMEMBER", userKey, userId) == 1 then
    redis.call("SREM", userKey, userId)
    redis.call("INCR", stockKey)
    return 1
end

return 0
