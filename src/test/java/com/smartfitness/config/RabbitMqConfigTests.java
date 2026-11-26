package com.smartfitness.config;

import com.smartfitness.dto.AppointmentCreateMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMqConfigTests {
    @Test
    void messageConverterRoundTripsLocalDateTime() {
        MessageConverter converter = new RabbitMqConfig().rabbitMessageConverter();
        LocalDateTime requestedAt = LocalDateTime.of(2026, 9, 17, 10, 30, 15);
        AppointmentCreateMessage source = new AppointmentCreateMessage("request-1", 1L, 2L, requestedAt, 1);

        Message message = converter.toMessage(source, new MessageProperties());
        Object restored = converter.fromMessage(message);

        assertTrue(restored instanceof AppointmentCreateMessage);
        assertEquals(requestedAt, ((AppointmentCreateMessage) restored).getRequestedAt());
    }
}
