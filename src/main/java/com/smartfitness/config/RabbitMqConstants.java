package com.smartfitness.config;

public final class RabbitMqConstants {
    public static final String APPOINTMENT_EXCHANGE = "smartfitness.appointment.exchange";
    public static final String APPOINTMENT_QUEUE = "smartfitness.appointment.queue";
    public static final String APPOINTMENT_ROUTING_KEY = "appointment.create";
    public static final String APPOINTMENT_DLX = "smartfitness.appointment.dlx";
    public static final String APPOINTMENT_DLQ = "smartfitness.appointment.dlq";
    public static final String APPOINTMENT_DEAD_ROUTING_KEY = "appointment.create.dead";

    private RabbitMqConstants() {
    }
}
