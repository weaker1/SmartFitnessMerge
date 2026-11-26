local metaKey = KEYS[1]
local usersKey = KEYS[2]
local userId = ARGV[1]
local now = tonumber(ARGV[2])
if redis.call('EXISTS', metaKey) == 0 then return 1 end
if redis.call('HGET', metaKey, 'status') ~= '1' then return 2 end
local startAt = tonumber(redis.call('HGET', metaKey, 'appointmentStart'))
local endAt = tonumber(redis.call('HGET', metaKey, 'appointmentEnd'))
if now < startAt then return 3 end
if now > endAt then return 4 end
if redis.call('SISMEMBER', usersKey, userId) == 1 then return 5 end
local stock = tonumber(redis.call('HGET', metaKey, 'stock'))
if not stock or stock <= 0 then return 6 end
redis.call('HINCRBY', metaKey, 'stock', -1)
redis.call('SADD', usersKey, userId)
local ttl = redis.call('PTTL', metaKey)
if ttl > 0 then redis.call('PEXPIRE', usersKey, ttl) end
return 0
