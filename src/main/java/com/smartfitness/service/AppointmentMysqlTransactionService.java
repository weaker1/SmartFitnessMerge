package com.smartfitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartfitness.dto.AppointmentCancellationResult;
import com.smartfitness.entity.AppointmentStatus;
import com.smartfitness.entity.TestAppointment;
import com.smartfitness.entity.TestSession;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.TestAppointmentMapper;
import com.smartfitness.mapper.TestSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AppointmentMysqlTransactionService {
    private final TestAppointmentMapper appointmentMapper;
    private final TestSessionMapper sessionMapper;

    public AppointmentMysqlTransactionService(TestAppointmentMapper appointmentMapper, TestSessionMapper sessionMapper) {
        this.appointmentMapper = appointmentMapper;
        this.sessionMapper = sessionMapper;
    }

    @Transactional
    public Long createAppointment(Long sessionId, Long userId, LocalDateTime now) {
        return createAppointment(java.util.UUID.randomUUID().toString(), sessionId, userId, now);
    }

    @Transactional
    public Long createAppointment(String requestId, Long sessionId, Long userId, LocalDateTime now) {
        TestAppointment byRequest = findByRequestId(requestId);
        if (byRequest != null) return byRequest.getId();
        TestAppointment existing = appointmentMapper.selectOne(new QueryWrapper<TestAppointment>()
                .eq("user_id", userId).eq("session_id", sessionId).last("LIMIT 1"));
        if (existing != null) return existing.getId();
        if (sessionMapper.decrementQuotaIfAvailable(sessionId, now) != 1) {
            throw new BusinessException("MySQL场次状态、窗口或库存校验失败");
        }
        TestAppointment appointment = new TestAppointment().setRequestId(requestId)
                .setUserId(userId).setSessionId(sessionId)
                .setStatus(AppointmentStatus.BOOKED.getCode()).setBookedAt(now);
        appointmentMapper.insert(appointment);
        return appointment.getId();
    }

    public TestAppointment findByRequestId(String requestId) {
        return appointmentMapper.selectOne(new QueryWrapper<TestAppointment>()
                .eq("request_id", requestId).last("LIMIT 1"));
    }

    public TestAppointment findByUserAndSession(Long userId, Long sessionId) {
        return appointmentMapper.selectOne(new QueryWrapper<TestAppointment>()
                .eq("user_id", userId).eq("session_id", sessionId).last("LIMIT 1"));
    }

    @Transactional
    public AppointmentCancellationResult cancelAppointment(Long appointmentId, Long userId, LocalDateTime now) {
        TestAppointment appointment = appointmentMapper.selectOne(new QueryWrapper<TestAppointment>()
                .eq("id", appointmentId).eq("user_id", userId).last("LIMIT 1"));
        if (appointment == null) throw new BusinessException("预约记录不存在");
        if (!Integer.valueOf(AppointmentStatus.BOOKED.getCode()).equals(appointment.getStatus())) {
            throw new BusinessException("预约已取消，不能重复取消");
        }
        TestSession session = sessionMapper.selectById(appointment.getSessionId());
        if (session == null) throw new BusinessException("体测场次不存在");
        if (!session.getStartTime().isAfter(now)) throw new BusinessException("场次已经开始，不能取消预约");
        if (appointmentMapper.cancelBookedAppointment(appointmentId, userId, now) != 1) {
            throw new BusinessException("预约已取消，不能重复取消");
        }
        if (sessionMapper.incrementQuota(session.getId()) != 1) throw new BusinessException("返还场次名额失败");
        return new AppointmentCancellationResult(session.getId(), userId, appointment.getRequestId());
    }
}
