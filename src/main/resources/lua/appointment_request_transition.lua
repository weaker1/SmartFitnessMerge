local current = redis.call('HGET', KEYS[1], 'status')
if not current then return -1 end
for i = 4, #ARGV do
  if current == ARGV[i] then
    redis.call('HSET', KEYS[1], 'status', ARGV[1])
    redis.call('HSET', KEYS[1], 'updatedAt', ARGV[2])
    redis.call('HSET', KEYS[1], 'reason', ARGV[3])
    return 1
  end
end
return 0
