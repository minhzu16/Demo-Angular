package com.tiki.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public Queue notificationQueue() {
        return new Queue("notification.queue", true);
    }

    @Bean
    public TopicExchange tikiExchange() {
        return new TopicExchange("tiki.events");
    }

    @Bean
    public Binding bindingNotification(Queue notificationQueue, TopicExchange tikiExchange) {
        return BindingBuilder.bind(notificationQueue).to(tikiExchange).with("notification.#");
    }
    
    @Bean
    public Binding bindingOrder(Queue notificationQueue, TopicExchange tikiExchange) {
        return BindingBuilder.bind(notificationQueue).to(tikiExchange).with("order.#");
    }

    @Bean
    public Binding bindingFlashSale(Queue notificationQueue, TopicExchange tikiExchange) {
        return BindingBuilder.bind(notificationQueue).to(tikiExchange).with("flashsale.#");
    }

    @Bean
    public TopicExchange warehouseExchange() {
        return new TopicExchange("warehouse.events");
    }

    @Bean
    public Binding bindingWarehouse(Queue notificationQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(notificationQueue).to(warehouseExchange).with("stock.#");
    }
}
