package com.smartfitness.service;

import com.smartfitness.dto.AppointmentRequestState;
import com.smartfitness.entity.AppointmentRequestStatus;
import com.smartfitness.utils.RedisConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AppointmentRequestStatusService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> transitionScript;

    public AppointmentRequestStatusService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.transitionScript = new DefaultRedisScript<>();
        this.transitionScript.setLocation(new ClassPathResource("lua/appointment_request_transition.lua"));
        this.transitionScript.setResultType(Long.class);
    }

    public void createPublishing(String requestId, Long userId, Long sessionId) {
        String key = key(requestId);
        Map<String, String> values = new HashMap<>();
        values.put("requestId", requestId);
        values.put("userId", userId.toString());
        values.put("sessionId", sessionId.toString());
        values.put("status", AppointmentRequestStatus.PUBLISHING.name());
        values.put("reason", "");
        values.put("updatedAt", Long.toString(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, RedisConstants.APPOINTMENT_REQUEST_TTL_HOURS, TimeUnit.HOURS);
    }

    public boolean markAccepted(String requestId) {
        return transition(requestId, AppointmentRequestStatus.ACCEPTED, "消息已由Broker接收",
                AppointmentRequestStatus.PUBLISHING);
    }

    public boolean markUnknown(String requestId, String reason) {
        return transition(requestId, AppointmentRequestStatus.UNKNOWN, reason,
                AppointmentRequestStatus.PUBLISHING);
    }

    public boolean markFailedByPublisher(String requestId, String reason) {
        return transition(requestId, AppointmentRequestStatus.FAILED, reason,
                AppointmentRequestStatus.PUBLISHING);
    }

    public boolean markFailedByDeadLetter(String requestId, String reason) {
        return transition(requestId, AppointmentRequestStatus.FAILED, reason,
                AppointmentRequestStatus.PUBLISHING, AppointmentRequestStatus.ACCEPTED,
                AppointmentRequestStatus.UNKNOWN, AppointmentRequestStatus.FAILED);
    }

    public boolean markBooked(String requestId) {
        return transition(requestId, AppointmentRequestStatus.BOOKED, "数据库预约已创建",
                AppointmentRequestStatus.PUBLISHING, AppointmentRequestStatus.ACCEPTED,
                AppointmentRequestStatus.UNKNOWN, AppointmentRequestStatus.BOOKED);
    }

    public boolean markCancelled(String requestId) {
        return transition(requestId, AppointmentRequestStatus.CANCELLED, "预约已取消",
                AppointmentRequestStatus.BOOKED, AppointmentRequestStatus.CANCELLED);
    }

    public AppointmentRequestState get(String requestId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(requestId));
        if (values == null || values.isEmpty()) return null;
        return new AppointmentRequestState().setRequestId(string(values.get("requestId")))
                .setUserId(number(values.get("userId"))).setSessionId(number(values.get("sessionId")))
                .setStatus(string(values.get("status"))).setReason(string(values.get("reason")))
                .setUpdatedAt(number(values.get("updatedAt")));
    }

    boolean transition(String requestId, AppointmentRequestStatus target, String reason,
                       AppointmentRequestStatus... allowed) {
        List<String> args = new ArrayList<>();
        args.add(target.name());
        args.add(Long.toString(System.currentTimeMillis()));
        args.add(reason == null ? "" : reason);
        Arrays.stream(allowed).map(Enum::name).forEach(args::add);
        Long result = redisTemplate.execute(transitionScript, Arrays.asList(key(requestId)),
                args.toArray(new String[0]));
        return Long.valueOf(1L).equals(result);
    }

    public String key(String requestId) { return RedisConstants.APPOINTMENT_REQUEST_KEY_PREFIX + requestId; }
    private String string(Object value) { return value == null ? null : value.toString(); }
    private Long number(Object value) { return value == null ? null : Long.valueOf(value.toString()); }
}
