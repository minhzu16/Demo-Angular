package com.tiki.shop.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShopRabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";

    @Bean(name = "shopNotificationQueue")
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE);
    }

    @Bean(name = "shopNotificationExchange")
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean(name = "shopNotificationBinding")
    public Binding notificationBinding(@Qualifier("shopNotificationQueue") Queue notificationQueue, 
                                       @Qualifier("shopNotificationExchange") TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean(name = "shopJsonMessageConverter")
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
