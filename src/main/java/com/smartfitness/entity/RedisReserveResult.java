package com.smartfitness.entity;

import com.smartfitness.exception.BusinessException;

public enum RedisReserveResult {
    SUCCESS(0, null), NOT_INITIALIZED(1, "预约场次缓存未初始化，请稍后重试"),
    NOT_PUBLISHED(2, "场次未发布或已关闭"), NOT_STARTED(3, "预约尚未开始"),
    ENDED(4, "预约已经结束"), DUPLICATE(5, "请勿重复预约"), NO_STOCK(6, "场次名额已满");

    private final long code;
    private final String message;
    RedisReserveResult(long code, String message) { this.code = code; this.message = message; }
    public long getCode() { return code; }
    public static RedisReserveResult fromCode(Long code) {
        for (RedisReserveResult value : values()) if (Long.valueOf(value.code).equals(code)) return value;
        throw new BusinessException("未知Redis预约结果");
    }
    public void throwIfFailed() { if (this != SUCCESS) throw new BusinessException(message); }
}
