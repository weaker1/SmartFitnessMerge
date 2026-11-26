package com.smartfitness.service;

import com.smartfitness.dto.TestSessionCreateDTO;
import com.smartfitness.dto.UserDTO;
import com.smartfitness.entity.TestItem;
import com.smartfitness.entity.TestSession;
import com.smartfitness.entity.TestSessionStatus;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.TestItemMapper;
import com.smartfitness.mapper.TestSessionMapper;
import com.smartfitness.service.impl.TestSessionServiceImpl;
import com.smartfitness.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestSessionServiceImplTests {
    private TestSessionMapper sessionMapper;
    private TestItemMapper itemMapper;
    private TestSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(TestSessionMapper.class);
        itemMapper = mock(TestItemMapper.class);
        service = new TestSessionServiceImpl(sessionMapper, itemMapper, mock(AppointmentRedisService.class));
    }

    @AfterEach
    void clear() {
        UserHolder.removeUser();
    }

    @Test
    void studentCannotCreateSession() {
        login(1L, "STUDENT");
        assertThrows(BusinessException.class, () -> service.createSession(validDto()));
        verify(sessionMapper, never()).insert(any(TestSession.class));
    }

    @Test
    void teacherCreatesDraftWithFullQuota() {
        login(2L, "TEACHER");
        when(itemMapper.selectById(1L)).thenReturn(new TestItem().setId(1L).setStatus(1));
        service.createSession(validDto());
        ArgumentCaptor<TestSession> captor = ArgumentCaptor.forClass(TestSession.class);
        verify(sessionMapper).insert(captor.capture());
        assertEquals(10, captor.getValue().getCapacity());
        assertEquals(10, captor.getValue().getAvailableQuota());
        assertEquals(TestSessionStatus.DRAFT.getCode(), captor.getValue().getStatus());
        assertEquals(2L, captor.getValue().getCreatedBy());
    }

    @Test
    void publishRechecksEnabledItem() {
        login(2L, "ADMIN");
        when(sessionMapper.selectById(3L)).thenReturn(session(TestSessionStatus.DRAFT, 10));
        when(itemMapper.selectById(1L)).thenReturn(new TestItem().setId(1L).setStatus(0));
        assertThrows(BusinessException.class, () -> service.publishSession(3L));
        verify(sessionMapper, never()).updateById(any(TestSession.class));
    }

    @Test
    void invalidTimeOrderIsRejected() {
        login(2L, "TEACHER");
        TestSessionCreateDTO dto = validDto();
        dto.setEndTime(dto.getStartTime().minusMinutes(1));
        assertThrows(BusinessException.class, () -> service.createSession(dto));
    }

    private TestSessionCreateDTO validDto() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        TestSessionCreateDTO dto = new TestSessionCreateDTO();
        dto.setItemId(1L);
        dto.setTitle("50米跑测试场次");
        dto.setStartTime(start);
        dto.setEndTime(start.plusHours(1));
        dto.setAppointmentStartTime(start.minusDays(1));
        dto.setAppointmentEndTime(start.minusHours(1));
        dto.setCapacity(10);
        return dto;
    }

    private TestSession session(TestSessionStatus status, int quota) {
        TestSessionCreateDTO dto = validDto();
        return new TestSession().setId(3L).setItemId(dto.getItemId()).setTitle(dto.getTitle())
                .setStartTime(dto.getStartTime()).setEndTime(dto.getEndTime())
                .setAppointmentStartTime(dto.getAppointmentStartTime())
                .setAppointmentEndTime(dto.getAppointmentEndTime())
                .setCapacity(dto.getCapacity()).setAvailableQuota(quota).setStatus(status.getCode());
    }

    private void login(Long id, String role) {
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setRole(role);
        UserHolder.saveUser(user);
    }
}
