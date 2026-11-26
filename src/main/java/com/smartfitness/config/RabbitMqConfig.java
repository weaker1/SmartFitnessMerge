package com.smartfitness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.ImmediateRequeueMessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;

@Configuration
public class RabbitMqConfig {
    @Bean
    public DirectExchange appointmentExchange() {
        return new DirectExchange(RabbitMqConstants.APPOINTMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange appointmentDeadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.APPOINTMENT_DLX, true, false);
    }

    @Bean
    public Queue appointmentQueue() {
        return QueueBuilder.durable(RabbitMqConstants.APPOINTMENT_QUEUE)
                .deadLetterExchange(RabbitMqConstants.APPOINTMENT_DLX)
                .deadLetterRoutingKey(RabbitMqConstants.APPOINTMENT_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue appointmentDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.APPOINTMENT_DLQ).build();
    }

    @Bean
    public Binding appointmentBinding() {
        return BindingBuilder.bind(appointmentQueue()).to(appointmentExchange())
                .with(RabbitMqConstants.APPOINTMENT_ROUTING_KEY);
    }

    @Bean
    public Binding appointmentDeadLetterBinding() {
        return BindingBuilder.bind(appointmentDeadLetterQueue()).to(appointmentDeadLetterExchange())
                .with(RabbitMqConstants.APPOINTMENT_DEAD_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = baseFactory(connectionFactory, converter);
        Advice retry = RetryInterceptorBuilder.stateless()
                .maxAttempts(3).backOffOptions(200L, 2.0, 1000L)
                .recoverer(new RejectAndDontRequeueRecoverer()).build();
        factory.setAdviceChain(retry);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory dlqListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = baseFactory(connectionFactory, converter);
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000L);
        Advice retry = RetryInterceptorBuilder.stateless().maxAttempts(5)
                .backOffPolicy(backOff).recoverer(new ImmediateRequeueMessageRecoverer()).build();
        factory.setAdviceChain(retry);
        factory.setDefaultRequeueRejected(true);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        return factory;
    }

    private SimpleRabbitListenerContainerFactory baseFactory(ConnectionFactory connectionFactory,
                                                              MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(4);
        factory.setPrefetchCount(1);
        return factory;
    }
}
