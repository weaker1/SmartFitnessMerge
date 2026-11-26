package com.smartfitness.service;

import com.smartfitness.exception.BusinessException;
import com.smartfitness.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
public class RedisRateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> slidingWindowScript;
    private final long loginWindowMs;
    private final int loginUserLimit;
    private final int loginIpLimit;
    private final long appointmentWindowMs;
    private final int appointmentUserLimit;

    public RedisRateLimitService(StringRedisTemplate redisTemplate,
                                 @Value("${smartfitness.rate-limit.login.window-ms:60000}") long loginWindowMs,
                                 @Value("${smartfitness.rate-limit.login.user-limit:8}") int loginUserLimit,
                                 @Value("${smartfitness.rate-limit.login.ip-limit:30}") int loginIpLimit,
                                 @Value("${smartfitness.rate-limit.appointment.window-ms:1000}") long appointmentWindowMs,
                                 @Value("${smartfitness.rate-limit.appointment.user-limit:3}") int appointmentUserLimit) {
        this.redisTemplate = redisTemplate;
        this.loginWindowMs = loginWindowMs;
        this.loginUserLimit = loginUserLimit;
        this.loginIpLimit = loginIpLimit;
        this.appointmentWindowMs = appointmentWindowMs;
        this.appointmentUserLimit = appointmentUserLimit;
        this.slidingWindowScript = new DefaultRedisScript<>();
        this.slidingWindowScript.setLocation(new ClassPathResource("lua/rate_limit_sliding_window.lua"));
        this.slidingWindowScript.setResultType(Long.class);
    }

    public void checkLogin(String username, String ip) {
        try {
            requireAllowed(RedisConstants.LOGIN_IP_RATE_KEY_PREFIX + normalize(ip), loginWindowMs, loginIpLimit);
            requireAllowed(RedisConstants.LOGIN_USER_RATE_KEY_PREFIX + normalize(username), loginWindowMs, loginUserLimit);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException ignored) {
            // Login rate limiting is protective only: Redis failure must not lock every user out.
        }
    }

    public void checkAppointment(Long userId) {
        try {
            requireAllowed(RedisConstants.APPOINTMENT_USER_RATE_KEY_PREFIX + userId,
                    appointmentWindowMs, appointmentUserLimit);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException("预约服务繁忙，请稍后重试");
        }
    }

    boolean isAllowed(String key, long now, long windowMs, int limit, String requestId) {
        Long result = redisTemplate.execute(slidingWindowScript, Collections.singletonList(key),
                Long.toString(now), Long.toString(windowMs), Integer.toString(limit), now + ":" + requestId);
        if (result == null) throw new IllegalStateException("限流脚本未返回结果");
        return Long.valueOf(1L).equals(result);
    }

    private void requireAllowed(String key, long windowMs, int limit) {
        if (!isAllowed(key, System.currentTimeMillis(), windowMs, limit, UUID.randomUUID().toString())) {
            throw new BusinessException("请求过于频繁，请稍后重试");
        }
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim().toLowerCase();
    }
}
