package com.smartfitness.mq;

import com.smartfitness.config.RabbitMqConstants;
import com.smartfitness.dto.AppointmentCreateMessage;
import com.smartfitness.entity.TestAppointment;
import com.smartfitness.service.AppointmentMysqlTransactionService;
import com.smartfitness.service.AppointmentRedisService;
import com.smartfitness.service.AppointmentRequestStatusService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentDeadLetterConsumer {
    private final AppointmentMysqlTransactionService transactionService;
    private final AppointmentRedisService redisService;
    private final AppointmentRequestStatusService statusService;

    public AppointmentDeadLetterConsumer(AppointmentMysqlTransactionService transactionService,
                                         AppointmentRedisService redisService,
                                         AppointmentRequestStatusService statusService) {
        this.transactionService = transactionService;
        this.redisService = redisService;
        this.statusService = statusService;
    }

    @RabbitListener(queues = RabbitMqConstants.APPOINTMENT_DLQ,
            containerFactory = "dlqListenerContainerFactory")
    public void consume(AppointmentCreateMessage message) {
        TestAppointment existing = transactionService.findByRequestId(message.getRequestId());
        if (existing == null) {
            existing = transactionService.findByUserAndSession(message.getUserId(), message.getSessionId());
        }
        if (existing != null) {
            if (!statusService.markBooked(message.getRequestId())) {
                throw new IllegalStateException("DLQ无法修复BOOKED状态");
            }
            return;
        }
        redisService.rollback(message.getSessionId(), message.getUserId());
        if (!statusService.markFailedByDeadLetter(message.getRequestId(), "消费重试耗尽，数据库未创建预约")) {
            throw new IllegalStateException("DLQ无法更新FAILED状态");
        }
    }
}
