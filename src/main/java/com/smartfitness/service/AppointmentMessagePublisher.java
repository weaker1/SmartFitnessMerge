package com.smartfitness.service;

import com.smartfitness.config.RabbitMqConstants;
import com.smartfitness.dto.AppointmentCreateMessage;
import com.smartfitness.dto.AppointmentRequestVO;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AppointmentMessagePublisher {
    private final RabbitTemplate rabbitTemplate;
    private final AppointmentRequestStatusService statusService;
    private final AppointmentRedisService redisService;
    private final long confirmTimeoutMillis;

    public AppointmentMessagePublisher(RabbitTemplate rabbitTemplate,
                                       AppointmentRequestStatusService statusService,
                                       AppointmentRedisService redisService,
                                       @Value("${smartfitness.appointment.confirm-timeout-ms:3000}") long confirmTimeoutMillis) {
        this.rabbitTemplate = rabbitTemplate;
        this.statusService = statusService;
        this.redisService = redisService;
        this.confirmTimeoutMillis = confirmTimeoutMillis;
    }

    public AppointmentRequestVO publish(AppointmentCreateMessage message) {
        CorrelationData correlation = new CorrelationData(message.getRequestId());
        try {
            rabbitTemplate.convertAndSend(RabbitMqConstants.APPOINTMENT_EXCHANGE,
                    RabbitMqConstants.APPOINTMENT_ROUTING_KEY, message, correlation);
        } catch (Exception e) {
            return definiteFailure(message, "消息发送失败: " + e.getMessage());
        }
        final CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            statusService.markUnknown(message.getRequestId(), "Publisher Confirm超时");
            return new AppointmentRequestVO(message.getRequestId(), "UNKNOWN", "预约请求状态确认中，请稍后查询");
        } catch (Exception e) {
            return definiteFailure(message, "Publisher Confirm失败: " + e.getMessage());
        }
        try {
            if (correlation.getReturnedMessage() != null) {
                return definiteFailure(message, "消息无法路由到预约队列");
            }
            if (!confirm.isAck()) {
                return definiteFailure(message, "Broker拒绝消息: " + confirm.getReason());
            }
            statusService.markAccepted(message.getRequestId());
            return new AppointmentRequestVO(message.getRequestId(), "ACCEPTED", "预约请求已受理");
        } catch (Exception e) {
            return new AppointmentRequestVO(message.getRequestId(), "UNKNOWN", "消息已确认但状态写入失败，请稍后查询");
        }
    }

    private AppointmentRequestVO definiteFailure(AppointmentCreateMessage message, String reason) {
        redisService.rollback(message.getSessionId(), message.getUserId());
        statusService.markFailedByPublisher(message.getRequestId(), reason);
        return new AppointmentRequestVO(message.getRequestId(), "FAILED", reason);
    }
}
