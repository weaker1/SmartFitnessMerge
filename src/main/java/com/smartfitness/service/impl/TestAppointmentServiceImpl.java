package com.smartfitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfitness.dto.SessionAppointmentVO;
import com.smartfitness.dto.AppointmentCancellationResult;
import com.smartfitness.dto.TestAppointmentVO;
import com.smartfitness.dto.AppointmentCreateMessage;
import com.smartfitness.dto.AppointmentRequestState;
import com.smartfitness.dto.AppointmentRequestVO;
import com.smartfitness.dto.UserDTO;
import com.smartfitness.entity.AppointmentStatus;
import com.smartfitness.entity.StudentProfile;
import com.smartfitness.entity.TestAppointment;
import com.smartfitness.entity.TestItem;
import com.smartfitness.entity.TestSession;
import com.smartfitness.entity.TestSessionStatus;
import com.smartfitness.entity.User;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.StudentProfileMapper;
import com.smartfitness.mapper.TestAppointmentMapper;
import com.smartfitness.mapper.TestItemMapper;
import com.smartfitness.mapper.TestSessionMapper;
import com.smartfitness.mapper.UserMapper;
import com.smartfitness.service.ITestAppointmentService;
import com.smartfitness.service.AppointmentMysqlTransactionService;
import com.smartfitness.service.AppointmentRedisService;
import com.smartfitness.service.AppointmentMessagePublisher;
import com.smartfitness.service.AppointmentRequestStatusService;
import com.smartfitness.service.RedisRateLimitService;
import com.smartfitness.utils.RoleUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class TestAppointmentServiceImpl extends ServiceImpl<TestAppointmentMapper, TestAppointment>
        implements ITestAppointmentService {
    private static final long MAX_PAGE_SIZE = 100L;

    private final TestAppointmentMapper appointmentMapper;
    private final TestSessionMapper sessionMapper;
    private final TestItemMapper itemMapper;
    private final UserMapper userMapper;
    private final StudentProfileMapper profileMapper;
    private final AppointmentRedisService redisService;
    private final AppointmentMysqlTransactionService transactionService;
    private final AppointmentRequestStatusService requestStatusService;
    private final AppointmentMessagePublisher messagePublisher;
    private final RedisRateLimitService rateLimitService;

    public TestAppointmentServiceImpl(TestAppointmentMapper appointmentMapper, TestSessionMapper sessionMapper,
                                      TestItemMapper itemMapper, UserMapper userMapper,
                                      StudentProfileMapper profileMapper, AppointmentRedisService redisService,
                                      AppointmentMysqlTransactionService transactionService,
                                      AppointmentRequestStatusService requestStatusService,
                                      AppointmentMessagePublisher messagePublisher,
                                      RedisRateLimitService rateLimitService) {
        this.appointmentMapper = appointmentMapper;
        this.sessionMapper = sessionMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.redisService = redisService;
        this.transactionService = transactionService;
        this.requestStatusService = requestStatusService;
        this.messagePublisher = messagePublisher;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public AppointmentRequestVO bookSession(Long sessionId) {
        UserDTO student = RoleUtils.requireStudent();
        rateLimitService.checkAppointment(student.getId());
        LocalDateTime now = LocalDateTime.now();
        redisService.reserve(sessionId, student.getId(),
                now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()).throwIfFailed();
        String requestId = UUID.randomUUID().toString();
        try {
            requestStatusService.createPublishing(requestId, student.getId(), sessionId);
            return messagePublisher.publish(new AppointmentCreateMessage(requestId, student.getId(),
                    sessionId, now, 1));
        } catch (RuntimeException e) {
            boolean compensated = false;
            for (int i = 0; i < 3 && !compensated; i++) {
                try { compensated = redisService.rollback(sessionId, student.getId()) || compensated; }
                catch (RuntimeException ignored) { }
            }
            if (!compensated) throw new BusinessException("数据库预约失败且Redis补偿失败，请人工核对");
            throw e;
        }
    }

    @Override
    public AppointmentRequestVO getRequestStatus(String requestId) {
        UserDTO student = RoleUtils.requireStudent();
        AppointmentRequestState state = requestStatusService.get(requestId);
        if (state != null) {
            if (!student.getId().equals(state.getUserId())) throw new BusinessException("无权查询该预约请求");
            return new AppointmentRequestVO(requestId, state.getStatus(), state.getReason());
        }
        TestAppointment appointment = transactionService.findByRequestId(requestId);
        if (appointment == null) throw new BusinessException("预约请求状态已过期或未知");
        if (!student.getId().equals(appointment.getUserId())) throw new BusinessException("无权查询该预约请求");
        String status = AppointmentStatus.fromCode(appointment.getStatus()).name();
        return new AppointmentRequestVO(requestId, status, "状态来自数据库");
    }

    @Override
    public void cancelAppointment(Long appointmentId) {
        UserDTO student = RoleUtils.requireStudent();
        LocalDateTime now = LocalDateTime.now();
        AppointmentCancellationResult result = transactionService.cancelAppointment(appointmentId, student.getId(), now);
        RuntimeException last = null;
        for (int i = 0; i < 3; i++) {
            try {
                redisService.cancel(result.getSessionId(), result.getUserId());
                if (result.getRequestId() != null) requestStatusService.markCancelled(result.getRequestId());
                return;
            }
            catch (RuntimeException e) { last = e; }
        }
        throw new BusinessException("数据库已取消，但Redis库存同步失败，请稍后核对");
    }

    @Override
    public Page<TestAppointmentVO> pageMyAppointments(long current, long pageSize) {
        UserDTO student = RoleUtils.requireStudent();
        validatePage(current, pageSize);
        Page<TestAppointment> page = appointmentMapper.selectPage(new Page<>(current, pageSize),
                new QueryWrapper<TestAppointment>().eq("user_id", student.getId())
                        .orderByDesc("booked_at", "id"));
        return toMyVoPage(page);
    }

    @Override
    public Page<SessionAppointmentVO> pageSessionAppointments(Long sessionId, long current, long pageSize) {
        RoleUtils.requireTeacherOrAdmin();
        requireSession(sessionId);
        validatePage(current, pageSize);
        Page<TestAppointment> page = appointmentMapper.selectPage(new Page<>(current, pageSize),
                new QueryWrapper<TestAppointment>().eq("session_id", sessionId)
                        .orderByAsc("booked_at", "id"));
        return toRosterVoPage(page);
    }

    private BusinessException bookingUnavailable(TestSession session, LocalDateTime now) {
        if (session == null) {
            return new BusinessException("体测场次不存在");
        }
        if (!Integer.valueOf(TestSessionStatus.PUBLISHED.getCode()).equals(session.getStatus())) {
            return new BusinessException("场次未发布或已关闭");
        }
        if (now.isBefore(session.getAppointmentStartTime())) {
            return new BusinessException("预约尚未开始");
        }
        if (now.isAfter(session.getAppointmentEndTime())) {
            return new BusinessException("预约已经结束");
        }
        if (session.getAvailableQuota() == null || session.getAvailableQuota() <= 0) {
            return new BusinessException("场次名额已满");
        }
        return new BusinessException("当前场次不可预约");
    }

    private TestSession requireSession(Long sessionId) {
        TestSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("体测场次不存在");
        }
        return session;
    }

    private Page<TestAppointmentVO> toMyVoPage(Page<TestAppointment> page) {
        List<Long> sessionIds = page.getRecords().stream().map(TestAppointment::getSessionId)
                .distinct().collect(Collectors.toList());
        Map<Long, TestSession> sessions = sessionIds.isEmpty() ? Collections.emptyMap()
                : sessionMapper.selectBatchIds(sessionIds).stream()
                .collect(Collectors.toMap(TestSession::getId, Function.identity()));
        List<Long> itemIds = sessions.values().stream().map(TestSession::getItemId)
                .distinct().collect(Collectors.toList());
        Map<Long, TestItem> items = itemIds.isEmpty() ? Collections.emptyMap()
                : itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(TestItem::getId, Function.identity()));
        Page<TestAppointmentVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(appointment -> {
            TestSession session = sessions.get(appointment.getSessionId());
            TestItem item = session == null ? null : items.get(session.getItemId());
            return TestAppointmentVO.from(appointment, session, item);
        }).collect(Collectors.toList()));
        return result;
    }

    private Page<SessionAppointmentVO> toRosterVoPage(Page<TestAppointment> page) {
        List<Long> userIds = page.getRecords().stream().map(TestAppointment::getUserId)
                .distinct().collect(Collectors.toList());
        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, StudentProfile> profiles = userIds.isEmpty() ? Collections.emptyMap()
                : profileMapper.selectList(new QueryWrapper<StudentProfile>().in("user_id", userIds)).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
        Page<SessionAppointmentVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(appointment -> SessionAppointmentVO.from(appointment,
                        users.get(appointment.getUserId()), profiles.get(appointment.getUserId())))
                .collect(Collectors.toList()));
        return result;
    }

    private void validatePage(long current, long pageSize) {
        if (current < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("分页参数不合法");
        }
    }
}
