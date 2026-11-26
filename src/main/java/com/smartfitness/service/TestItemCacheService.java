package com.smartfitness.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfitness.dto.TestItemDTO;
import com.smartfitness.entity.TestItem;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.TestItemMapper;
import com.smartfitness.utils.RedisConstants;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class TestItemCacheService {
    private static final Logger log = LoggerFactory.getLogger(TestItemCacheService.class);
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final TestItemMapper itemMapper;
    private final ObjectMapper objectMapper;
    private final long ttlMinutes;
    private final long jitterMinutes;
    private final long nullTtlMinutes;
    private final long lockWaitSeconds;
    private final long lockLeaseSeconds;

    public TestItemCacheService(StringRedisTemplate redisTemplate, RedissonClient redissonClient,
                                TestItemMapper itemMapper, ObjectMapper objectMapper,
                                @Value("${smartfitness.cache.test-item.ttl-minutes:30}") long ttlMinutes,
                                @Value("${smartfitness.cache.test-item.jitter-minutes:5}") long jitterMinutes,
                                @Value("${smartfitness.cache.test-item.null-ttl-minutes:2}") long nullTtlMinutes,
                                @Value("${smartfitness.cache.test-item.lock-wait-seconds:2}") long lockWaitSeconds,
                                @Value("${smartfitness.cache.test-item.lock-lease-seconds:10}") long lockLeaseSeconds) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.itemMapper = itemMapper;
        this.objectMapper = objectMapper;
        this.ttlMinutes = ttlMinutes;
        this.jitterMinutes = jitterMinutes;
        this.nullTtlMinutes = nullTtlMinutes;
        this.lockWaitSeconds = lockWaitSeconds;
        this.lockLeaseSeconds = lockLeaseSeconds;
    }

    public TestItemDTO getById(Long id) {
        String key = cacheKey(id);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) return decode(cached);

        RLock lock = redissonClient.getLock(lockKey(id));
        boolean acquired = false;
        try {
            acquired = lock.tryLock(lockWaitSeconds, lockLeaseSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null) return decode(cached);
                throw new BusinessException("缓存重建繁忙，请稍后重试");
            }
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null) return decode(cached);

            TestItem item = itemMapper.selectById(id);
            if (item == null) {
                redisTemplate.opsForValue().set(key, RedisConstants.CACHE_NULL_VALUE,
                        nullTtlMinutes, TimeUnit.MINUTES);
                throw new BusinessException("体测项目不存在");
            }
            TestItemDTO dto = TestItemDTO.from(item);
            redisTemplate.opsForValue().set(key, encode(dto), normalTtlMinutes(), TimeUnit.MINUTES);
            return dto;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("缓存重建被中断，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    public void evictAfterCommit(Long id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { evictWithRetry(id); }
            });
        } else {
            evictWithRetry(id);
        }
    }

    void evictWithRetry(Long id) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                redisTemplate.delete(cacheKey(id));
                return;
            } catch (RuntimeException e) {
                last = e;
                log.warn("删除TestItem缓存失败，id={}，attempt={}", id, attempt, e);
            }
        }
        log.error("数据库已提交但TestItem缓存删除失败，存在短暂不一致窗口，id={}", id, last);
    }

    long normalTtlMinutes() {
        return ttlMinutes + (jitterMinutes <= 0 ? 0 : ThreadLocalRandom.current().nextLong(jitterMinutes + 1));
    }

    private TestItemDTO decode(String value) {
        if (RedisConstants.CACHE_NULL_VALUE.equals(value)) throw new BusinessException("体测项目不存在");
        try { return objectMapper.readValue(value, TestItemDTO.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("TestItem缓存数据损坏", e); }
    }

    private String encode(TestItemDTO value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("TestItem缓存序列化失败", e); }
    }

    public String cacheKey(Long id) { return RedisConstants.TEST_ITEM_CACHE_KEY_PREFIX + id; }
    public String lockKey(Long id) { return RedisConstants.TEST_ITEM_LOCK_KEY_PREFIX + id; }
}
