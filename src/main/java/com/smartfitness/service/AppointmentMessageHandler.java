package com.smartfitness.service;

import com.smartfitness.dto.AppointmentCreateMessage;
import com.smartfitness.entity.TestAppointment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class AppointmentMessageHandler {
    private final AppointmentMysqlTransactionService transactionService;
    private final AppointmentRequestStatusService statusService;

    public AppointmentMessageHandler(AppointmentMysqlTransactionService transactionService,
                                     AppointmentRequestStatusService statusService) {
        this.transactionService = transactionService;
        this.statusService = statusService;
    }

    public void handle(AppointmentCreateMessage message) {
        try {
            transactionService.createAppointment(message.getRequestId(), message.getSessionId(),
                    message.getUserId(), message.getRequestedAt());
        } catch (DuplicateKeyException duplicate) {
            TestAppointment existing = transactionService.findByRequestId(message.getRequestId());
            if (existing == null) {
                existing = transactionService.findByUserAndSession(message.getUserId(), message.getSessionId());
            }
            if (existing == null) throw duplicate;
        }
        if (!statusService.markBooked(message.getRequestId())) {
            throw new IllegalStateException("无法将预约请求状态更新为BOOKED");
        }
    }
}
