local removed = redis.call('SREM', KEYS[2], ARGV[1])
if removed == 1 then
  redis.call('HINCRBY', KEYS[1], 'stock', 1)
end
return removed
