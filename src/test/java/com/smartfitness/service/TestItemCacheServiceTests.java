package com.smartfitness.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfitness.dto.TestItemDTO;
import com.smartfitness.entity.TestItem;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.TestItemMapper;
import com.smartfitness.utils.RedisConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestItemCacheServiceTests {
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private RedissonClient redisson;
    private RLock lock;
    private TestItemMapper mapper;
    private TestItemCacheService service;
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    @BeforeEach void setUp() throws Exception {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        redisson = mock(RedissonClient.class);
        lock = mock(RLock.class);
        mapper = mock(TestItemMapper.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        when(redis.opsForValue()).thenReturn(values);
        when(redisson.getLock(any())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        service = new TestItemCacheService(redis, redisson, mapper, objectMapper,
                30, 5, 2, 2, 10);
    }

    @Test void cacheHitDoesNotAccessDatabase() throws Exception {
        TestItemDTO dto = TestItemDTO.from(item());
        when(values.get(service.cacheKey(1L))).thenReturn(objectMapper.writeValueAsString(dto));
        assertEquals("50米", service.getById(1L).getName());
        verify(mapper, never()).selectById(any());
        verify(redisson, never()).getLock(any());
    }

    @Test void missLoadsDatabaseWritesCacheAndUnlocksInFinally() {
        when(values.get(service.cacheKey(1L))).thenReturn(null);
        when(mapper.selectById(1L)).thenReturn(item());
        assertEquals("50米", service.getById(1L).getName());
        verify(values).set(eq(service.cacheKey(1L)), any(), anyLong(), eq(TimeUnit.MINUTES));
        verify(lock).unlock();
    }

    @Test void nullMarkerPreventsPenetrationAndUsesShorterTtl() {
        when(values.get(service.cacheKey(99L))).thenReturn(null);
        when(mapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getById(99L));
        verify(values).set(service.cacheKey(99L), RedisConstants.CACHE_NULL_VALUE, 2, TimeUnit.MINUTES);
        when(values.get(service.cacheKey(99L))).thenReturn(RedisConstants.CACHE_NULL_VALUE);
        assertThrows(BusinessException.class, () -> service.getById(99L));
        verify(mapper, times(1)).selectById(99L);
        assertTrue(2 < service.normalTtlMinutes());
    }

    @Test void lockHolderDoubleChecksAndSkipsDatabaseWhenAnotherThreadFilledCache() throws Exception {
        String json = objectMapper.writeValueAsString(TestItemDTO.from(item()));
        when(values.get(service.cacheKey(1L))).thenReturn(null, json);
        assertEquals("50米", service.getById(1L).getName());
        verify(mapper, never()).selectById(any());
        verify(lock).unlock();
    }

    @Test void nonLockHolderNeverRebuildsDatabase() throws Exception {
        when(values.get(service.cacheKey(1L))).thenReturn(null);
        when(lock.tryLock(anyLong(), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.getById(1L));
        verify(mapper, never()).selectById(any());
        verify(lock, never()).unlock();
    }

    @Test void evictionRunsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.evictAfterCommit(1L);
            verify(redis, never()).delete(any(String.class));
            List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();
            callbacks.forEach(TransactionSynchronization::afterCommit);
            verify(redis).delete(service.cacheKey(1L));
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test void deleteFailureRetriesThreeTimes() {
        when(redis.delete(service.cacheKey(1L))).thenThrow(new IllegalStateException("down"));
        service.evictWithRetry(1L);
        verify(redis, times(3)).delete(service.cacheKey(1L));
    }

    private TestItem item() {
        return new TestItem().setId(1L).setName("50米").setDescription("短跑").setUnit("s")
                .setStatus(1).setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now());
    }
}
