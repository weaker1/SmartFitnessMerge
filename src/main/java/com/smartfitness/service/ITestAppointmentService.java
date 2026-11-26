package com.smartfitness.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartfitness.dto.SessionAppointmentVO;
import com.smartfitness.dto.TestAppointmentVO;
import com.smartfitness.dto.AppointmentRequestVO;
import com.smartfitness.entity.TestAppointment;

public interface ITestAppointmentService extends IService<TestAppointment> {
    AppointmentRequestVO bookSession(Long sessionId);
    AppointmentRequestVO getRequestStatus(String requestId);
    void cancelAppointment(Long appointmentId);
    Page<TestAppointmentVO> pageMyAppointments(long current, long pageSize);
    Page<SessionAppointmentVO> pageSessionAppointments(Long sessionId, long current, long pageSize);
}
