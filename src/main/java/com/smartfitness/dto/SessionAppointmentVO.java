package com.smartfitness.dto;

import com.smartfitness.entity.AppointmentStatus;
import com.smartfitness.entity.StudentProfile;
import com.smartfitness.entity.TestAppointment;
import com.smartfitness.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionAppointmentVO {
    private Long appointmentId;
    private Long studentUserId;
    private String realName;
    private String studentNo;
    private String status;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;

    public static SessionAppointmentVO from(TestAppointment appointment, User user, StudentProfile profile) {
        SessionAppointmentVO vo = new SessionAppointmentVO();
        vo.setAppointmentId(appointment.getId());
        vo.setStudentUserId(appointment.getUserId());
        vo.setRealName(user == null ? null : user.getRealName());
        vo.setStudentNo(profile == null ? null : profile.getStudentNo());
        vo.setStatus(AppointmentStatus.fromCode(appointment.getStatus()).name());
        vo.setBookedAt(appointment.getBookedAt());
        vo.setCancelledAt(appointment.getCancelledAt());
        return vo;
    }
}
