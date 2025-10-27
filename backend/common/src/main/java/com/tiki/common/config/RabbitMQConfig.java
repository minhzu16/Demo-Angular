package com.tiki.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration
 * Sprint 9 - Day 7-9
 * 
 * Defines exchanges, queues, and bindings for async event processing
 */
@Configuration
public class RabbitMQConfig {
    
    // Exchange names
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PRODUCT_EXCHANGE = "product.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    
    // Queue names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_UPDATED_QUEUE = "order.updated.queue";
    public static final String PRODUCT_UPDATED_QUEUE = "product.updated.queue";
    public static final String NOTIFICATION_EMAIL_QUEUE = "notification.email.queue";
    public static final String NOTIFICATION_SMS_QUEUE = "notification.sms.queue";
    
    // Routing keys
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_UPDATED_KEY = "order.updated";
    public static final String PRODUCT_UPDATED_KEY = "product.updated";
    public static final String NOTIFICATION_EMAIL_KEY = "notification.email";
    public static final String NOTIFICATION_SMS_KEY = "notification.sms";
    
    /**
     * JSON Message Converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
    
    // ==================== ORDER EXCHANGE & QUEUES ====================
    
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }
    
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
            .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE + ".dlx")
            .build();
    }
    
    @Bean
    public Queue orderUpdatedQueue() {
        return QueueBuilder.durable(ORDER_UPDATED_QUEUE)
            .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE + ".dlx")
            .build();
    }
    
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
            .bind(orderCreatedQueue())
            .to(orderExchange())
            .with(ORDER_CREATED_KEY);
    }
    
    @Bean
    public Binding orderUpdatedBinding() {
        return BindingBuilder
            .bind(orderUpdatedQueue())
            .to(orderExchange())
            .with(ORDER_UPDATED_KEY);
    }
    
    // ==================== PRODUCT EXCHANGE & QUEUES ====================
    
    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE);
    }
    
    @Bean
    public Queue productUpdatedQueue() {
        return QueueBuilder.durable(PRODUCT_UPDATED_QUEUE)
            .withArgument("x-dead-letter-exchange", PRODUCT_EXCHANGE + ".dlx")
            .build();
    }
    
    @Bean
    public Binding productUpdatedBinding() {
        return BindingBuilder
            .bind(productUpdatedQueue())
            .to(productExchange())
            .with(PRODUCT_UPDATED_KEY);
    }
    
    // ==================== NOTIFICATION EXCHANGE & QUEUES ====================
    
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }
    
    @Bean
    public Queue notificationEmailQueue() {
        return QueueBuilder.durable(NOTIFICATION_EMAIL_QUEUE)
            .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE + ".dlx")
            .build();
    }
    
    @Bean
    public Queue notificationSmsQueue() {
        return QueueBuilder.durable(NOTIFICATION_SMS_QUEUE)
            .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE + ".dlx")
            .build();
    }
    
    @Bean
    public Binding notificationEmailBinding() {
        return BindingBuilder
            .bind(notificationEmailQueue())
            .to(notificationExchange())
            .with(NOTIFICATION_EMAIL_KEY);
    }
    
    @Bean
    public Binding notificationSmsBinding() {
        return BindingBuilder
            .bind(notificationSmsQueue())
            .to(notificationExchange())
            .with(NOTIFICATION_SMS_KEY);
    }
}
