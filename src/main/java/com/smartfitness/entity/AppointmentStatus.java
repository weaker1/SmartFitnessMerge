package com.smartfitness.entity;

import com.smartfitness.exception.BusinessException;

public enum AppointmentStatus {
    BOOKED(1), CANCELLED(2);

    private final int code;

    AppointmentStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AppointmentStatus fromCode(Integer code) {
        for (AppointmentStatus status : values()) {
            if (Integer.valueOf(status.code).equals(code)) {
                return status;
            }
        }
        throw new BusinessException("未知预约状态");
    }
}
