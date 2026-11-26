package com.smartfitness.entity;

import com.smartfitness.exception.BusinessException;

public enum TestSessionStatus {
    DRAFT(0), PUBLISHED(1), CLOSED(2), FINISHED(3), CANCELLED(4);

    private final int code;

    TestSessionStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TestSessionStatus fromCode(Integer code) {
        for (TestSessionStatus status : values()) {
            if (Integer.valueOf(status.code).equals(code)) {
                return status;
            }
        }
        throw new BusinessException("未知场次状态");
    }
}
