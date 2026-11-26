package com.smartfitness.utils;

public class RedisConstants {
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL_MINUTES = 30L;

    public static final Long CACHE_NULL_TTL = 2L;
    public static final String CACHE_REBUILD_LOCK_KEY = "lock:cache:rebuild:";

    public static final String APPOINTMENT_KEY_PREFIX = "smartfitness:appointment:";
    public static final String APPOINTMENT_META_SUFFIX = ":meta";
    public static final String APPOINTMENT_USERS_SUFFIX = ":users";
    public static final String APPOINTMENT_CANCELLED_SUFFIX = ":cancelled";
    public static final Long APPOINTMENT_TTL_BUFFER_HOURS = 24L;
    public static final String APPOINTMENT_REQUEST_KEY_PREFIX = "smartfitness:appointment:request:";
    public static final Long APPOINTMENT_REQUEST_TTL_HOURS = 48L;

    public static final String TEST_ITEM_CACHE_KEY_PREFIX = "smartfitness:cache:test-item:";
    public static final String TEST_ITEM_LOCK_KEY_PREFIX = "smartfitness:lock:test-item:";
    public static final String CACHE_NULL_VALUE = "__NULL__";
    public static final String LOGIN_USER_RATE_KEY_PREFIX = "smartfitness:rate:login:user:";
    public static final String LOGIN_IP_RATE_KEY_PREFIX = "smartfitness:rate:login:ip:";
    public static final String APPOINTMENT_USER_RATE_KEY_PREFIX = "smartfitness:rate:appointment:user:";

}
