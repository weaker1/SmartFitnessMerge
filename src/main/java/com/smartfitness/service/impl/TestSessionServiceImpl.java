package com.smartfitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfitness.dto.TestSessionCreateDTO;
import com.smartfitness.dto.TestSessionUpdateDTO;
import com.smartfitness.dto.TestSessionVO;
import com.smartfitness.entity.TestItem;
import com.smartfitness.entity.TestSession;
import com.smartfitness.entity.TestSessionStatus;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.TestItemMapper;
import com.smartfitness.mapper.TestSessionMapper;
import com.smartfitness.service.ITestSessionService;
import com.smartfitness.service.AppointmentRedisService;
import com.smartfitness.utils.RoleUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TestSessionServiceImpl extends ServiceImpl<TestSessionMapper, TestSession> implements ITestSessionService {
    private static final long MAX_PAGE_SIZE = 100L;

    private final TestSessionMapper sessionMapper;
    private final TestItemMapper itemMapper;
    private final AppointmentRedisService redisService;

    public TestSessionServiceImpl(TestSessionMapper sessionMapper, TestItemMapper itemMapper,
                                  AppointmentRedisService redisService) {
        this.sessionMapper = sessionMapper;
        this.itemMapper = itemMapper;
        this.redisService = redisService;
    }

    @Override
    public Long createSession(TestSessionCreateDTO dto) {
        Long operatorId = RoleUtils.requireTeacherOrAdmin().getId();
        validate(dto);
        TestSession session = copyFields(new TestSession(), dto)
                .setAvailableQuota(dto.getCapacity())
                .setStatus(TestSessionStatus.DRAFT.getCode())
                .setCreatedBy(operatorId);
        sessionMapper.insert(session);
        return session.getId();
    }

    @Override
    public void updateDraftSession(Long id, TestSessionUpdateDTO dto) {
        RoleUtils.requireTeacherOrAdmin();
        TestSession current = requireSession(id);
        if (!Integer.valueOf(TestSessionStatus.DRAFT.getCode()).equals(current.getStatus())) {
            throw new BusinessException("只有草稿场次可以修改");
        }
        validate(dto);
        TestSession update = copyFields(new TestSession().setId(id), dto)
                .setAvailableQuota(dto.getCapacity());
        sessionMapper.updateById(update);
    }

    @Override
    @Transactional
    public void publishSession(Long id) {
        RoleUtils.requireTeacherOrAdmin();
        TestSession session = requireSession(id);
        if (!Integer.valueOf(TestSessionStatus.DRAFT.getCode()).equals(session.getStatus())) {
            throw new BusinessException("只有草稿场次可以发布");
        }
        validate(session);
        sessionMapper.updateById(new TestSession().setId(id).setStatus(TestSessionStatus.PUBLISHED.getCode()));
        session.setStatus(TestSessionStatus.PUBLISHED.getCode());
        redisService.initialize(session);
        cleanupRedisIfTransactionRollsBack(id);
    }

    @Override
    @Transactional
    public void closeSession(Long id) {
        RoleUtils.requireTeacherOrAdmin();
        TestSession session = requireSession(id);
        if (!Integer.valueOf(TestSessionStatus.PUBLISHED.getCode()).equals(session.getStatus())) {
            throw new BusinessException("只有已发布场次可以关闭");
        }
        sessionMapper.updateById(new TestSession().setId(id).setStatus(TestSessionStatus.CLOSED.getCode()));
        redisService.updateStatus(id, TestSessionStatus.CLOSED.getCode());
        restoreRedisStatusIfTransactionRollsBack(id, session.getStatus());
    }

    @Override
    @Transactional
    public void cancelSession(Long id) {
        RoleUtils.requireTeacherOrAdmin();
        TestSession session = requireSession(id);
        if (Integer.valueOf(TestSessionStatus.FINISHED.getCode()).equals(session.getStatus())
                || Integer.valueOf(TestSessionStatus.CANCELLED.getCode()).equals(session.getStatus())) {
            throw new BusinessException("当前场次不能取消");
        }
        sessionMapper.updateById(new TestSession().setId(id).setStatus(TestSessionStatus.CANCELLED.getCode()));
        if (Integer.valueOf(TestSessionStatus.PUBLISHED.getCode()).equals(session.getStatus())) {
            redisService.updateStatus(id, TestSessionStatus.CANCELLED.getCode());
            restoreRedisStatusIfTransactionRollsBack(id, session.getStatus());
        }
    }

    @Override
    public TestSessionVO getSession(Long id) {
        TestSession session = requireSession(id);
        return TestSessionVO.from(session, itemMapper.selectById(session.getItemId()));
    }

    @Override
    public Page<TestSessionVO> pageVisibleSessions(Long itemId, long current, long pageSize) {
        RoleUtils.currentUser();
        validatePage(current, pageSize);
        QueryWrapper<TestSession> wrapper = new QueryWrapper<TestSession>()
                .eq("status", TestSessionStatus.PUBLISHED.getCode())
                .gt("start_time", LocalDateTime.now())
                .eq(itemId != null, "item_id", itemId)
                .orderByAsc("start_time", "id");
        Page<TestSession> page = sessionMapper.selectPage(new Page<>(current, pageSize), wrapper);
        return toVoPage(page);
    }

    private void validate(TestSessionCreateDTO dto) {
        validateTimes(dto.getStartTime(), dto.getEndTime(), dto.getAppointmentStartTime(),
                dto.getAppointmentEndTime(), dto.getCapacity());
        requireEnabledItem(dto.getItemId());
    }

    private void validate(TestSession session) {
        validateTimes(session.getStartTime(), session.getEndTime(), session.getAppointmentStartTime(),
                session.getAppointmentEndTime(), session.getCapacity());
        requireEnabledItem(session.getItemId());
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end, LocalDateTime appointmentStart,
                               LocalDateTime appointmentEnd, Integer capacity) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException("场次开始时间必须早于结束时间");
        }
        if (appointmentStart == null || appointmentEnd == null || !appointmentStart.isBefore(appointmentEnd)) {
            throw new BusinessException("预约开始时间必须早于预约结束时间");
        }
        if (appointmentEnd.isAfter(start)) {
            throw new BusinessException("预约结束时间不能晚于场次开始时间");
        }
        if (capacity == null || capacity <= 0) {
            throw new BusinessException("场次容量必须大于0");
        }
    }

    private TestItem requireEnabledItem(Long itemId) {
        TestItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("体测项目不存在");
        }
        if (!Integer.valueOf(1).equals(item.getStatus())) {
            throw new BusinessException("体测项目已停用");
        }
        return item;
    }

    private TestSession requireSession(Long id) {
        TestSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException("体测场次不存在");
        }
        return session;
    }

    private TestSession copyFields(TestSession target, TestSessionCreateDTO dto) {
        return target.setItemId(dto.getItemId())
                .setTitle(dto.getTitle().trim())
                .setStartTime(dto.getStartTime())
                .setEndTime(dto.getEndTime())
                .setAppointmentStartTime(dto.getAppointmentStartTime())
                .setAppointmentEndTime(dto.getAppointmentEndTime())
                .setCapacity(dto.getCapacity());
    }

    private Page<TestSessionVO> toVoPage(Page<TestSession> page) {
        List<Long> itemIds = page.getRecords().stream().map(TestSession::getItemId)
                .distinct().collect(Collectors.toList());
        Map<Long, TestItem> items = itemIds.isEmpty() ? Collections.emptyMap()
                : itemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(TestItem::getId, Function.identity()));
        Page<TestSessionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(session -> TestSessionVO.from(session, items.get(session.getItemId())))
                .collect(Collectors.toList()));
        return result;
    }

    private void validatePage(long current, long pageSize) {
        if (current < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("分页参数不合法");
        }
    }

    private void cleanupRedisIfTransactionRollsBack(Long sessionId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) redisService.delete(sessionId);
            }
        });
    }

    private void restoreRedisStatusIfTransactionRollsBack(Long sessionId, Integer oldStatus) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) redisService.updateStatus(sessionId, oldStatus);
            }
        });
    }
}
