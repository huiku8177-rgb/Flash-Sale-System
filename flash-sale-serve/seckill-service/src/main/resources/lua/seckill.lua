-- KEYS[1] = 库存 key, 例如 seckill:stock:1
-- KEYS[2] = 用户抢购记录 key, 例如 seckill:user:1
-- ARGV[1] = userId

local stockKey = KEYS[1]
local userKey = KEYS[2]
local userId = ARGV[1]

-- 1. 判断用户是否已经抢购过
if redis.call("SISMEMBER", userKey, userId) == 1 then
	return 2
end

-- 2. 读取库存
local stock = tonumber(redis.call("GET", stockKey))

-- 没有库存 key 或库存不足
if stock == nil or stock <= 0 then
	return 0
end

-- 3. 扣减库存
redis.call("DECR", stockKey)

-- 4. 记录用户已抢购
redis.call("SADD", userKey, userId)

-- 5. 返回成功
return 1