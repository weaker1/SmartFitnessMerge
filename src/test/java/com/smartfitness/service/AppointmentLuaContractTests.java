package com.smartfitness.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentLuaContractTests {
    @Test
    void reserveOwnsAdmissionAndMarksUserAfterStockDecrement() throws IOException {
        String script = script("appointment_reserve.lua");

        assertTrue(script.contains("EXISTS"));
        assertTrue(script.contains("SISMEMBER"));
        assertTrue(script.contains("HINCRBY', metaKey, 'stock', -1"));
        assertTrue(script.contains("SADD', usersKey, userId"));
        assertFalse(script.contains("SREM"));
    }

    @Test
    void rollbackOnlyRestoresStockWhenReservationUserWasRemoved() throws IOException {
        String script = script("appointment_rollback.lua");

        assertTrue(script.contains("local removed = redis.call('SREM'"));
        assertTrue(script.contains("if removed == 1 then"));
        assertTrue(script.contains("HINCRBY', KEYS[1], 'stock', 1"));
        assertFalse(script.contains("SADD"));
    }

    @Test
    void normalCancelKeepsUsersAndRestoresStockOnlyOnFirstCancel() throws IOException {
        String script = script("appointment_cancel.lua");

        assertTrue(script.contains("SISMEMBER"));
        assertTrue(script.contains("local firstCancel = redis.call('SADD'"));
        assertTrue(script.contains("if firstCancel == 1 then"));
        assertTrue(script.contains("HINCRBY', KEYS[1], 'stock', 1"));
        assertFalse(script.contains("SREM"));
    }

    @Test
    void requestTransitionUsesRedis32CompatibleSingleFieldHsetCalls() throws IOException {
        String script = script("appointment_request_transition.lua");

        assertTrue(script.contains("'status', ARGV[1])"));
        assertTrue(script.contains("'updatedAt', ARGV[2])"));
        assertTrue(script.contains("'reason', ARGV[3])"));
        assertFalse(script.contains("'status', ARGV[1], 'updatedAt'"));
    }

    private String script(String name) throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource("lua/" + name).getInputStream(), StandardCharsets.UTF_8);
    }
}
