package com.smartfitness.dto;

import com.smartfitness.entity.TestItem;
import com.smartfitness.entity.TestSession;
import com.smartfitness.entity.TestSessionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestSessionVO {
    private Long sessionId;
    private Long itemId;
    private String itemName;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime appointmentStartTime;
    private LocalDateTime appointmentEndTime;
    private Integer capacity;
    private Integer availableQuota;
    private String status;

    public static TestSessionVO from(TestSession session, TestItem item) {
        TestSessionVO vo = new TestSessionVO();
        vo.setSessionId(session.getId());
        vo.setItemId(session.getItemId());
        vo.setItemName(item == null ? null : item.getName());
        vo.setTitle(session.getTitle());
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());
        vo.setAppointmentStartTime(session.getAppointmentStartTime());
        vo.setAppointmentEndTime(session.getAppointmentEndTime());
        vo.setCapacity(session.getCapacity());
        vo.setAvailableQuota(session.getAvailableQuota());
        vo.setStatus(TestSessionStatus.fromCode(session.getStatus()).name());
        return vo;
    }
}
