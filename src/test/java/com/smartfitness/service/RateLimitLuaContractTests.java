package com.smartfitness.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitLuaContractTests {
    @Test void slidingWindowOperationsAreAtomicAndMemberComesFromCaller() throws Exception {
        String script = StreamUtils.copyToString(new ClassPathResource(
                "lua/rate_limit_sliding_window.lua").getInputStream(), StandardCharsets.UTF_8);
        assertTrue(script.contains("ZREMRANGEBYSCORE"));
        assertTrue(script.contains("ZCARD"));
        assertTrue(script.contains("ZADD', key, now, member"));
        assertTrue(script.contains("PEXPIRE"));
        assertTrue(script.contains("local member = ARGV[4]"));
    }
}
