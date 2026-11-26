package com.smartfitness.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AppointmentRequestState {
    private String requestId;
    private Long userId;
    private Long sessionId;
    private String status;
    private String reason;
    private Long updatedAt;
}
