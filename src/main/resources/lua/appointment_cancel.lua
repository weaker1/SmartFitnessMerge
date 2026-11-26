if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 0 then return 0 end
local firstCancel = redis.call('SADD', KEYS[3], ARGV[1])
if firstCancel == 1 then
  redis.call('HINCRBY', KEYS[1], 'stock', 1)
  local ttl = redis.call('PTTL', KEYS[1])
  if ttl > 0 then redis.call('PEXPIRE', KEYS[3], ttl) end
end
return firstCancel
