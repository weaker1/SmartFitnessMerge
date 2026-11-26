package com.smartfitness.service;

import com.smartfitness.dto.AppointmentCreateMessage;
import com.smartfitness.entity.TestAppointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentMessageHandlerTests {
    private AppointmentMysqlTransactionService transactionService;
    private AppointmentRequestStatusService statusService;
    private AppointmentMessageHandler handler;

    @BeforeEach
    void setUp() {
        transactionService = mock(AppointmentMysqlTransactionService.class);
        statusService = mock(AppointmentRequestStatusService.class);
        handler = new AppointmentMessageHandler(transactionService, statusService);
        when(statusService.markBooked("request-1")).thenReturn(true);
    }

    @Test
    void normalConsumeCreatesDatabaseAppointmentThenMarksBooked() {
        handler.handle(message());
        verify(transactionService).createAppointment(anyString(), anyLong(), anyLong(), any(LocalDateTime.class));
        verify(statusService).markBooked("request-1");
    }

    @Test
    void duplicateRequestIsIdempotentSuccess() {
        doThrow(new DuplicateKeyException("duplicate")).when(transactionService)
                .createAppointment(anyString(), anyLong(), anyLong(), any(LocalDateTime.class));
        when(transactionService.findByRequestId("request-1"))
                .thenReturn(new TestAppointment().setId(9L).setRequestId("request-1"));
        handler.handle(message());
        verify(statusService).markBooked("request-1");
    }

    @Test
    void duplicateBusinessAppointmentIsAlsoIdempotentSuccess() {
        doThrow(new DuplicateKeyException("duplicate")).when(transactionService)
                .createAppointment(anyString(), anyLong(), anyLong(), any(LocalDateTime.class));
        when(transactionService.findByUserAndSession(1L, 5L))
                .thenReturn(new TestAppointment().setId(9L).setUserId(1L).setSessionId(5L));
        handler.handle(message());
        verify(statusService).markBooked("request-1");
    }

    @Test
    void redisStatusFailureCausesRetryAndDatabaseRemainsIdempotent() {
        when(statusService.markBooked("request-1")).thenReturn(false, true);
        assertThrows(IllegalStateException.class, () -> handler.handle(message()));
        handler.handle(message());
        verify(transactionService, times(2))
                .createAppointment(anyString(), anyLong(), anyLong(), any(LocalDateTime.class));
        verify(statusService, times(2)).markBooked("request-1");
    }

    private AppointmentCreateMessage message() {
        return new AppointmentCreateMessage("request-1", 1L, 5L, LocalDateTime.now(), 1);
    }
}
