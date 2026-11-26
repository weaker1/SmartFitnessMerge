package com.smartfitness.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfitness.dto.TestRecordCreateDTO;
import com.smartfitness.dto.TestRecordVO;
import com.smartfitness.dto.UserDTO;
import com.smartfitness.entity.StudentProfile;
import com.smartfitness.entity.TestItem;
import com.smartfitness.entity.TestRecord;
import com.smartfitness.entity.User;
import com.smartfitness.entity.UserRole;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.StudentProfileMapper;
import com.smartfitness.mapper.TestItemMapper;
import com.smartfitness.mapper.TestRecordMapper;
import com.smartfitness.mapper.UserMapper;
import com.smartfitness.service.ITestRecordService;
import com.smartfitness.utils.RoleUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TestRecordServiceImpl extends ServiceImpl<TestRecordMapper, TestRecord> implements ITestRecordService {
    private static final long MAX_PAGE_SIZE = 100L;

    private final TestRecordMapper testRecordMapper;
    private final TestItemMapper testItemMapper;
    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;

    public TestRecordServiceImpl(TestRecordMapper testRecordMapper, TestItemMapper testItemMapper,
                                 UserMapper userMapper, StudentProfileMapper studentProfileMapper) {
        this.testRecordMapper = testRecordMapper;
        this.testItemMapper = testItemMapper;
        this.userMapper = userMapper;
        this.studentProfileMapper = studentProfileMapper;
    }

    @Override
    @Transactional
    public Long createRecord(TestRecordCreateDTO dto) {
        UserDTO operator = RoleUtils.requireTeacherOrAdmin();
        requireStudent(dto.getUserId());
        TestItem item = testItemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw new BusinessException("体测项目不存在");
        }
        if (!Integer.valueOf(1).equals(item.getStatus())) {
            throw new BusinessException("体测项目已停用");
        }
        TestRecord record = new TestRecord()
                .setUserId(dto.getUserId())
                .setItemId(dto.getItemId())
                .setScore(dto.getScore())
                .setVideoUrl(dto.getVideoUrl())
                .setDataUrl(dto.getDataUrl())
                .setTestTime(dto.getTestTime())
                .setCreatedBy(operator.getId());
        testRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public Page<TestRecordVO> pageMyRecords(Long itemId, long current, long pageSize) {
        UserDTO student = RoleUtils.requireStudent();
        return queryRecords(student.getId(), itemId, null, null, current, pageSize);
    }

    @Override
    public Page<TestRecordVO> pageStudentRecords(Long studentUserId, Long itemId,
                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                 long current, long pageSize) {
        RoleUtils.requireTeacherOrAdmin();
        requireStudent(studentUserId);
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        return queryRecords(studentUserId, itemId, startTime, endTime, current, pageSize);
    }

    private Page<TestRecordVO> queryRecords(Long userId, Long itemId, LocalDateTime startTime,
                                            LocalDateTime endTime, long current, long pageSize) {
        if (current < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("分页参数不合法");
        }
        QueryWrapper<TestRecord> wrapper = new QueryWrapper<TestRecord>()
                .eq("user_id", userId)
                .eq(itemId != null, "item_id", itemId)
                .ge(startTime != null, "test_time", startTime)
                .le(endTime != null, "test_time", endTime)
                .orderByDesc("test_time", "id");
        Page<TestRecord> recordPage = testRecordMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<Long> itemIds = recordPage.getRecords().stream().map(TestRecord::getItemId)
                .distinct().collect(Collectors.toList());
        Map<Long, TestItem> itemMap = itemIds.isEmpty() ? Collections.emptyMap()
                : testItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(TestItem::getId, Function.identity()));
        Page<TestRecordVO> result = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        result.setRecords(recordPage.getRecords().stream()
                .map(record -> TestRecordVO.from(record, itemMap.get(record.getItemId())))
                .collect(Collectors.toList()));
        return result;
    }

    private void requireStudent(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !UserRole.STUDENT.equals(user.getRole()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException("学生用户不存在或已禁用");
        }
        Integer profileCount = studentProfileMapper.selectCount(new QueryWrapper<StudentProfile>()
                .eq("user_id", userId));
        if (profileCount == null || profileCount == 0) {
            throw new BusinessException("学生档案不存在");
        }
    }
}
