package com.smartfitness.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCreateMessage implements Serializable {
    private String requestId;
    private Long userId;
    private Long sessionId;
    private LocalDateTime requestedAt;
    private Integer schemaVersion;
}
