package com.smartfitness.mq;

import com.smartfitness.dto.AppointmentCreateMessage;
import com.smartfitness.entity.TestAppointment;
import com.smartfitness.service.AppointmentMysqlTransactionService;
import com.smartfitness.service.AppointmentRedisService;
import com.smartfitness.service.AppointmentRequestStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentDeadLetterConsumerTests {
    private AppointmentMysqlTransactionService transactionService;
    private AppointmentRedisService redisService;
    private AppointmentRequestStatusService statusService;
    private AppointmentDeadLetterConsumer consumer;

    @BeforeEach
    void setUp() {
        transactionService = mock(AppointmentMysqlTransactionService.class);
        redisService = mock(AppointmentRedisService.class);
        statusService = mock(AppointmentRequestStatusService.class);
        consumer = new AppointmentDeadLetterConsumer(transactionService, redisService, statusService);
        when(statusService.markBooked("request-1")).thenReturn(true);
        when(statusService.markFailedByDeadLetter(org.mockito.ArgumentMatchers.eq("request-1"),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
    }

    @Test
    void databaseExistingRequestRepairsBookedWithoutRollback() {
        when(transactionService.findByRequestId("request-1"))
                .thenReturn(new TestAppointment().setId(9L).setRequestId("request-1"));
        consumer.consume(message());
        verify(statusService).markBooked("request-1");
        verify(redisService, never()).rollback(anyLong(), anyLong());
    }

    @Test
    void databaseMissingRollsBackAndMarksFailed() {
        when(redisService.rollback(5L, 1L)).thenReturn(true);
        consumer.consume(message());
        verify(redisService).rollback(5L, 1L);
        verify(statusService).markFailedByDeadLetter(org.mockito.ArgumentMatchers.eq("request-1"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void repeatedDeadLetterReliesOnIdempotentRollback() {
        when(redisService.rollback(5L, 1L)).thenReturn(true, false);
        consumer.consume(message());
        consumer.consume(message());
        verify(redisService, times(2)).rollback(5L, 1L);
    }

    private AppointmentCreateMessage message() {
        return new AppointmentCreateMessage("request-1", 1L, 5L, LocalDateTime.now(), 1);
    }
}
