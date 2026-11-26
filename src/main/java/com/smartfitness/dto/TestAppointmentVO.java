package com.smartfitness.dto;

import com.smartfitness.entity.AppointmentStatus;
import com.smartfitness.entity.TestAppointment;
import com.smartfitness.entity.TestItem;
import com.smartfitness.entity.TestSession;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestAppointmentVO {
    private Long appointmentId;
    private Long sessionId;
    private Long itemId;
    private String itemName;
    private String sessionTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;

    public static TestAppointmentVO from(TestAppointment appointment, TestSession session, TestItem item) {
        TestAppointmentVO vo = new TestAppointmentVO();
        vo.setAppointmentId(appointment.getId());
        vo.setSessionId(appointment.getSessionId());
        vo.setStatus(AppointmentStatus.fromCode(appointment.getStatus()).name());
        vo.setBookedAt(appointment.getBookedAt());
        vo.setCancelledAt(appointment.getCancelledAt());
        if (session != null) {
            vo.setItemId(session.getItemId());
            vo.setSessionTitle(session.getTitle());
            vo.setStartTime(session.getStartTime());
            vo.setEndTime(session.getEndTime());
        }
        vo.setItemName(item == null ? null : item.getName());
        return vo;
    }
}
