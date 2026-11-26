package com.smartfitness.utils;

import cn.hutool.core.util.StrUtil;

public final class AuthorizationUtils {
    private static final String BEARER_PREFIX = "Bearer ";

    private AuthorizationUtils() {
    }

    public static String extractBearerToken(String authorization) {
        if (StrUtil.isBlank(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return StrUtil.isBlank(token) ? null : token;
    }
}
