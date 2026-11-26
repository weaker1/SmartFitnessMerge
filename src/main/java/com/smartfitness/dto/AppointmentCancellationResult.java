package com.smartfitness.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentCancellationResult {
    private Long sessionId;
    private Long userId;
    private String requestId;

    public AppointmentCancellationResult(Long sessionId, Long userId) {
        this(sessionId, userId, null);
    }
}
