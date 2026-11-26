package com.smartfitness.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfitness.dto.TestRecordCreateDTO;
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
import com.smartfitness.service.impl.TestRecordServiceImpl;
import com.smartfitness.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestRecordServiceImplTests {
    private TestRecordMapper recordMapper;
    private TestItemMapper itemMapper;
    private UserMapper userMapper;
    private StudentProfileMapper profileMapper;
    private TestRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        recordMapper = mock(TestRecordMapper.class);
        itemMapper = mock(TestItemMapper.class);
        userMapper = mock(UserMapper.class);
        profileMapper = mock(StudentProfileMapper.class);
        service = new TestRecordServiceImpl(recordMapper, itemMapper, userMapper, profileMapper);
    }

    @AfterEach
    void clearUser() {
        UserHolder.removeUser();
    }

    @Test
    void studentCannotCreateRecord() {
        login(1L, "STUDENT");
        assertThrows(BusinessException.class, () -> service.createRecord(createDto()));
        verify(recordMapper, never()).insert(any(TestRecord.class));
    }

    @Test
    void teacherCanCreateRecordAndCreatedByIsTrustedContextValue() {
        login(20L, "TEACHER");
        validStudentAndItem();
        service.createRecord(createDto());

        ArgumentCaptor<TestRecord> captor = ArgumentCaptor.forClass(TestRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertEquals(20L, captor.getValue().getCreatedBy());
        assertEquals(1L, captor.getValue().getUserId());
    }

    @Test
    void adminCanCreateRecord() {
        login(30L, "ADMIN");
        validStudentAndItem();
        service.createRecord(createDto());
        verify(recordMapper).insert(any(TestRecord.class));
    }

    @Test
    void disabledItemRejectsNewRecord() {
        login(20L, "TEACHER");
        validStudent();
        when(itemMapper.selectById(2L)).thenReturn(new TestItem().setId(2L).setStatus(0));
        assertThrows(BusinessException.class, () -> service.createRecord(createDto()));
        verify(recordMapper, never()).insert(any(TestRecord.class));
    }

    @Test
    void nonStudentTargetIsRejected() {
        login(20L, "TEACHER");
        when(userMapper.selectById(1L)).thenReturn(new User().setId(1L).setRole(UserRole.TEACHER).setStatus(1));
        assertThrows(BusinessException.class, () -> service.createRecord(createDto()));
    }

    @Test
    void missingUserIsRejected() {
        login(20L, "TEACHER");
        when(userMapper.selectById(1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.createRecord(createDto()));
        verify(recordMapper, never()).insert(any(TestRecord.class));
    }

    @Test
    void missingItemIsRejected() {
        login(20L, "TEACHER");
        validStudent();
        when(itemMapper.selectById(2L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.createRecord(createDto()));
        verify(recordMapper, never()).insert(any(TestRecord.class));
    }

    @Test
    void myRecordsUsesCurrentStudentWithoutCallerSuppliedUserId() {
        login(77L, "STUDENT");
        Page<TestRecord> empty = new Page<>(1, 10, 0);
        empty.setRecords(Collections.emptyList());
        when(recordMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(empty);

        service.pageMyRecords(null, 1, 10);

        verify(recordMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void teacherQueryRequiresExistingStudentProfile() {
        login(20L, "TEACHER");
        when(userMapper.selectById(anyLong())).thenReturn(new User().setId(1L).setRole(UserRole.STUDENT).setStatus(1));
        when(profileMapper.selectCount(any(Wrapper.class))).thenReturn(0);
        assertThrows(BusinessException.class,
                () -> service.pageStudentRecords(1L, null, null, null, 1, 10));
    }

    private void validStudentAndItem() {
        validStudent();
        when(itemMapper.selectById(2L)).thenReturn(new TestItem().setId(2L).setStatus(1));
    }

    private void validStudent() {
        when(userMapper.selectById(1L)).thenReturn(new User().setId(1L).setRole(UserRole.STUDENT).setStatus(1));
        when(profileMapper.selectCount(any(Wrapper.class))).thenReturn(1);
    }

    private TestRecordCreateDTO createDto() {
        TestRecordCreateDTO dto = new TestRecordCreateDTO();
        dto.setUserId(1L);
        dto.setItemId(2L);
        dto.setScore(new BigDecimal("18.50"));
        dto.setTestTime(LocalDateTime.now().minusMinutes(1));
        return dto;
    }

    private void login(Long id, String role) {
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setRole(role);
        UserHolder.saveUser(user);
    }
}
