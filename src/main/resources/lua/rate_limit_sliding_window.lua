local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local member = ARGV[4]

redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMs)
local count = redis.call('ZCARD', key)
if count >= limit then
  redis.call('PEXPIRE', key, windowMs * 2)
  return 0
end
redis.call('ZADD', key, now, member)
redis.call('PEXPIRE', key, windowMs * 2)
return 1
