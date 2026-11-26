package com.smartfitness.service;

import com.smartfitness.dto.AppointmentCancellationResult;
import com.smartfitness.entity.AppointmentStatus;
import com.smartfitness.entity.TestAppointment;
import com.smartfitness.entity.TestSession;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.TestAppointmentMapper;
import com.smartfitness.mapper.TestSessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentMysqlTransactionServiceTests {
    private TestAppointmentMapper appointmentMapper;
    private TestSessionMapper sessionMapper;
    private AppointmentMysqlTransactionService service;

    @BeforeEach
    void setUp() {
        appointmentMapper = mock(TestAppointmentMapper.class);
        sessionMapper = mock(TestSessionMapper.class);
        service = new AppointmentMysqlTransactionService(appointmentMapper, sessionMapper);
    }

    @Test
    void successfulCreateConditionallyDecrementsQuotaAndInsertsAppointment() {
        when(sessionMapper.decrementQuotaIfAvailable(anyLong(), any(LocalDateTime.class))).thenReturn(1);
        when(appointmentMapper.insert(any(TestAppointment.class))).thenAnswer(invocation -> {
            invocation.<TestAppointment>getArgument(0).setId(9L);
            return 1;
        });

        assertEquals(9L, service.createAppointment(5L, 1L, LocalDateTime.now()));

        verify(sessionMapper).decrementQuotaIfAvailable(anyLong(), any(LocalDateTime.class));
        verify(appointmentMapper).insert(any(TestAppointment.class));
    }

    @Test
    void existingAppointmentIsIdempotentBeforeQuotaChange() {
        when(appointmentMapper.selectOne(any())).thenReturn(bookedAppointment());

        assertEquals(9L, service.createAppointment(5L, 1L, LocalDateTime.now()));

        verify(sessionMapper, never()).decrementQuotaIfAvailable(anyLong(), any(LocalDateTime.class));
        verify(appointmentMapper, never()).insert(any(TestAppointment.class));
    }

    @Test
    void failedConditionalQuotaUpdatePreventsInsert() {
        when(sessionMapper.decrementQuotaIfAvailable(anyLong(), any(LocalDateTime.class))).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.createAppointment(5L, 1L, LocalDateTime.now()));

        verify(appointmentMapper, never()).insert(any(TestAppointment.class));
    }

    @Test
    void databaseUniqueConstraintPropagatesSoTransactionCanRollback() {
        when(sessionMapper.decrementQuotaIfAvailable(anyLong(), any(LocalDateTime.class))).thenReturn(1);
        when(appointmentMapper.insert(any(TestAppointment.class)))
                .thenThrow(new DuplicateKeyException("uk_appointment_user_session"));

        assertThrows(DuplicateKeyException.class,
                () -> service.createAppointment(5L, 1L, LocalDateTime.now()));
    }

    @Test
    void createAndCancelRemainTransactionBoundaries() throws Exception {
        Method create = AppointmentMysqlTransactionService.class.getMethod(
                "createAppointment", Long.class, Long.class, LocalDateTime.class);
        Method cancel = AppointmentMysqlTransactionService.class.getMethod(
                "cancelAppointment", Long.class, Long.class, LocalDateTime.class);

        assertTrue(create.isAnnotationPresent(Transactional.class));
        assertTrue(cancel.isAnnotationPresent(Transactional.class));
    }

    @Test
    void successfulCancelUsesBookedCasThenReturnsQuotaOnce() {
        when(appointmentMapper.selectOne(any())).thenReturn(bookedAppointment());
        when(sessionMapper.selectById(5L)).thenReturn(futureSession());
        when(appointmentMapper.cancelBookedAppointment(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(sessionMapper.incrementQuota(5L)).thenReturn(1);

        AppointmentCancellationResult result = service.cancelAppointment(9L, 1L, LocalDateTime.now());

        assertEquals(5L, result.getSessionId());
        verify(sessionMapper).incrementQuota(5L);
    }

    @Test
    void repeatedCancelDoesNotReturnQuota() {
        when(appointmentMapper.selectOne(any())).thenReturn(
                bookedAppointment().setStatus(AppointmentStatus.CANCELLED.getCode()));

        assertThrows(BusinessException.class,
                () -> service.cancelAppointment(9L, 1L, LocalDateTime.now()));

        verify(appointmentMapper, never())
                .cancelBookedAppointment(anyLong(), anyLong(), any(LocalDateTime.class));
        verify(sessionMapper, never()).incrementQuota(anyLong());
    }

    @Test
    void startedSessionCannotBeCancelled() {
        when(appointmentMapper.selectOne(any())).thenReturn(bookedAppointment());
        when(sessionMapper.selectById(5L)).thenReturn(
                futureSession().setStartTime(LocalDateTime.now().minusMinutes(1)));

        assertThrows(BusinessException.class,
                () -> service.cancelAppointment(9L, 1L, LocalDateTime.now()));

        verify(appointmentMapper, never())
                .cancelBookedAppointment(anyLong(), anyLong(), any(LocalDateTime.class));
        verify(sessionMapper, never()).incrementQuota(anyLong());
    }

    @Test
    void losingCancelCasDoesNotReturnQuota() {
        when(appointmentMapper.selectOne(any())).thenReturn(bookedAppointment());
        when(sessionMapper.selectById(5L)).thenReturn(futureSession());
        when(appointmentMapper.cancelBookedAppointment(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.cancelAppointment(9L, 1L, LocalDateTime.now()));

        verify(sessionMapper, never()).incrementQuota(anyLong());
    }

    @Test
    void missingAppointmentOrSessionCannotBeCancelled() {
        assertThrows(BusinessException.class,
                () -> service.cancelAppointment(9L, 1L, LocalDateTime.now()));

        when(appointmentMapper.selectOne(any())).thenReturn(bookedAppointment());
        when(sessionMapper.selectById(5L)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> service.cancelAppointment(9L, 1L, LocalDateTime.now()));

        verify(appointmentMapper, never())
                .cancelBookedAppointment(anyLong(), anyLong(), any(LocalDateTime.class));
    }

    private TestAppointment bookedAppointment() {
        return new TestAppointment().setId(9L).setUserId(1L).setSessionId(5L)
                .setStatus(AppointmentStatus.BOOKED.getCode())
                .setBookedAt(LocalDateTime.now().minusMinutes(1));
    }

    private TestSession futureSession() {
        return new TestSession().setId(5L).setCapacity(10).setAvailableQuota(9)
                .setStartTime(LocalDateTime.now().plusDays(1));
    }
}
