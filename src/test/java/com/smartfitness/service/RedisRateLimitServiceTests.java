package com.smartfitness.service;

import com.smartfitness.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimitServiceTests {
    private StringRedisTemplate redis;
    private RedisRateLimitService service;

    @BeforeEach void setUp() {
        redis = mock(StringRedisTemplate.class);
        service = new RedisRateLimitService(redis, 60000, 8, 30, 1000, 3);
    }

    @Test void loginIpLimitRejectsBeforeUsernameCheck() {
        when(redis.execute(any(), anyList(), any(), any(), any(), any())).thenReturn(0L);
        assertThrows(BusinessException.class, () -> service.checkLogin("alice", "127.0.0.1"));
        verify(redis, times(1)).execute(any(), anyList(), any(), any(), any(), any());
    }

    @Test void loginUsernameLimitRejectsAfterIpAllows() {
        when(redis.execute(any(), anyList(), any(), any(), any(), any())).thenReturn(1L, 0L);
        assertThrows(BusinessException.class, () -> service.checkLogin("alice", "127.0.0.1"));
        verify(redis, times(2)).execute(any(), anyList(), any(), any(), any(), any());
    }

    @Test void loginRedisFailureFailsOpen() {
        when(redis.execute(any(), anyList(), any(), any(), any(), any())).thenThrow(new IllegalStateException("down"));
        assertDoesNotThrow(() -> service.checkLogin("alice", "127.0.0.1"));
    }

    @Test void appointmentLimitRejectsAndRedisFailureFailsClosed() {
        when(redis.execute(any(), anyList(), any(), any(), any(), any())).thenReturn(0L);
        assertThrows(BusinessException.class, () -> service.checkAppointment(1L));
        when(redis.execute(any(), anyList(), any(), any(), any(), any())).thenThrow(new IllegalStateException("down"));
        assertThrows(BusinessException.class, () -> service.checkAppointment(1L));
    }

    @Test void slidingWindowCanAllowAgainAfterOldEntriesExpire() {
        when(redis.execute(any(), anyList(), any(), any(), any(), any())).thenReturn(0L, 1L);
        assertThrows(BusinessException.class, () -> service.checkAppointment(1L));
        assertDoesNotThrow(() -> service.checkAppointment(1L));
    }
}
