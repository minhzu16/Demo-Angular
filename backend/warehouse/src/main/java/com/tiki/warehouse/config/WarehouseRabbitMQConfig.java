package com.tiki.warehouse.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WarehouseRabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "tiki.events";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String WAREHOUSE_QUEUE = "warehouse.order.created.queue";

    public static final String WAREHOUSE_EVENTS_EXCHANGE = "warehouse.events";
    public static final String STOCK_RESERVED_ROUTING_KEY = "stock.reserved";
    public static final String STOCK_FAILED_ROUTING_KEY = "stock.failed";

    @Bean
    public Queue warehouseQueue() {
        return new Queue(WAREHOUSE_QUEUE, true);
    }

    @Bean
    public TopicExchange tikiEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE);
    }

    @Bean
    public Binding warehouseBinding(Queue warehouseQueue, TopicExchange tikiEventsExchange) {
        return BindingBuilder.bind(warehouseQueue).to(tikiEventsExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange warehouseEventsExchange() {
        return new TopicExchange(WAREHOUSE_EVENTS_EXCHANGE);
    }

}
