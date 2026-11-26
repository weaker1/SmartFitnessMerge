package com.smartfitness.service;

import com.smartfitness.entity.RedisReserveResult;
import com.smartfitness.entity.TestSession;
import com.smartfitness.utils.RedisConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AppointmentRedisService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveScript;
    private final DefaultRedisScript<Long> rollbackScript;
    private final DefaultRedisScript<Long> cancelScript;

    public AppointmentRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        reserveScript = script("lua/appointment_reserve.lua");
        rollbackScript = script("lua/appointment_rollback.lua");
        cancelScript = script("lua/appointment_cancel.lua");
    }

    public RedisReserveResult reserve(Long sessionId, Long userId, long nowMillis) {
        Long result = redisTemplate.execute(reserveScript, Arrays.asList(metaKey(sessionId), usersKey(sessionId)),
                userId.toString(), Long.toString(nowMillis));
        return RedisReserveResult.fromCode(result);
    }

    public boolean rollback(Long sessionId, Long userId) {
        Long result = redisTemplate.execute(rollbackScript, Arrays.asList(metaKey(sessionId), usersKey(sessionId)),
                userId.toString());
        return Long.valueOf(1L).equals(result);
    }

    public boolean cancel(Long sessionId, Long userId) {
        Long result = redisTemplate.execute(cancelScript,
                Arrays.asList(metaKey(sessionId), usersKey(sessionId), cancelledKey(sessionId)), userId.toString());
        return Long.valueOf(1L).equals(result);
    }

    public void initialize(TestSession session) {
        String meta = metaKey(session.getId());
        String users = usersKey(session.getId());
        String cancelled = cancelledKey(session.getId());
        redisTemplate.delete(Arrays.asList(meta, users, cancelled));
        Map<String, String> values = new HashMap<>();
        values.put("stock", session.getAvailableQuota().toString());
        values.put("status", session.getStatus().toString());
        values.put("appointmentStart", Long.toString(toEpochMilli(session.getAppointmentStartTime())));
        values.put("appointmentEnd", Long.toString(toEpochMilli(session.getAppointmentEndTime())));
        redisTemplate.opsForHash().putAll(meta, values);
        long ttlMillis = Math.max(1L, Duration.between(LocalDateTime.now(),
                session.getAppointmentEndTime().plusHours(RedisConstants.APPOINTMENT_TTL_BUFFER_HOURS)).toMillis());
        redisTemplate.expire(meta, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public void updateStatus(Long sessionId, int status) {
        redisTemplate.opsForHash().put(metaKey(sessionId), "status", Integer.toString(status));
    }

    public void delete(Long sessionId) {
        redisTemplate.delete(Arrays.asList(metaKey(sessionId), usersKey(sessionId), cancelledKey(sessionId)));
    }

    public String metaKey(Long id) { return RedisConstants.APPOINTMENT_KEY_PREFIX + "{" + id + "}" + RedisConstants.APPOINTMENT_META_SUFFIX; }
    public String usersKey(Long id) { return RedisConstants.APPOINTMENT_KEY_PREFIX + "{" + id + "}" + RedisConstants.APPOINTMENT_USERS_SUFFIX; }
    public String cancelledKey(Long id) { return RedisConstants.APPOINTMENT_KEY_PREFIX + "{" + id + "}" + RedisConstants.APPOINTMENT_CANCELLED_SUFFIX; }

    private long toEpochMilli(LocalDateTime time) { return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); }
    private DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
