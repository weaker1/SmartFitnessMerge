package com.smartfitness.service;

import com.smartfitness.dto.AppointmentCreateMessage;
import com.smartfitness.dto.AppointmentRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentMessagePublisherTests {
    private RabbitTemplate rabbitTemplate;
    private AppointmentRequestStatusService statusService;
    private AppointmentRedisService redisService;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        statusService = mock(AppointmentRequestStatusService.class);
        redisService = mock(AppointmentRedisService.class);
        when(redisService.rollback(5L, 1L)).thenReturn(true);
    }

    @Test
    void ackMarksAcceptedWithoutRollback() {
        completeConfirm(true, null, false);
        AppointmentRequestVO result = publisher(1000).publish(message());
        assertEquals("ACCEPTED", result.getStatus());
        verify(statusService).markAccepted("request-1");
        verify(redisService, never()).rollback(anyLong(), anyLong());
    }

    @Test
    void nackRollsBackAndMarksFailed() {
        completeConfirm(false, "nack", false);
        AppointmentRequestVO result = publisher(1000).publish(message());
        assertEquals("FAILED", result.getStatus());
        verify(redisService).rollback(5L, 1L);
        verify(statusService).markFailedByPublisher(any(), any());
    }

    @Test
    void returnedMessageRollsBackAndMarksFailed() {
        completeConfirm(true, null, true);
        AppointmentRequestVO result = publisher(1000).publish(message());
        assertEquals("FAILED", result.getStatus());
        verify(redisService).rollback(5L, 1L);
    }

    @Test
    void sendExceptionRollsBackAndMarksFailed() {
        doThrow(new IllegalStateException("connection refused")).when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(AppointmentCreateMessage.class),
                        any(CorrelationData.class));
        AppointmentRequestVO result = publisher(1000).publish(message());
        assertEquals("FAILED", result.getStatus());
        verify(redisService).rollback(5L, 1L);
    }

    @Test
    void confirmTimeoutBecomesUnknownWithoutRollback() {
        AppointmentRequestVO result = publisher(1).publish(message());
        assertEquals("UNKNOWN", result.getStatus());
        verify(statusService).markUnknown(any(), any());
        verify(redisService, never()).rollback(anyLong(), anyLong());
    }

    private void completeConfirm(boolean ack, String reason, boolean returned) {
        doAnswer(invocation -> {
            CorrelationData data = invocation.getArgument(3);
            if (returned) data.setReturnedMessage(new Message(new byte[0], new MessageProperties()));
            data.getFuture().set(new CorrelationData.Confirm(ack, reason));
            return null;
        }).when(rabbitTemplate).convertAndSend(any(String.class), any(String.class),
                any(AppointmentCreateMessage.class), any(CorrelationData.class));
    }

    private AppointmentMessagePublisher publisher(long timeout) {
        return new AppointmentMessagePublisher(rabbitTemplate, statusService, redisService, timeout);
    }

    private AppointmentCreateMessage message() {
        return new AppointmentCreateMessage("request-1", 1L, 5L, LocalDateTime.now(), 1);
    }
}
