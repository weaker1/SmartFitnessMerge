package com.smartfitness.mq;

import com.smartfitness.config.RabbitMqConstants;
import com.smartfitness.dto.AppointmentCreateMessage;
import com.smartfitness.service.AppointmentMessageHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentConsumer {
    private final AppointmentMessageHandler handler;

    public AppointmentConsumer(AppointmentMessageHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = RabbitMqConstants.APPOINTMENT_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void consume(AppointmentCreateMessage message) {
        handler.handle(message);
    }
}
