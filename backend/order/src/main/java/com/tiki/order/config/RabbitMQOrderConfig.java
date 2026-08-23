package com.tiki.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQOrderConfig {

    public static final String WAREHOUSE_EVENTS_EXCHANGE = "warehouse.events";
    public static final String STOCK_QUEUE = "order.stock.response.queue";

    @Bean
    public Queue stockQueue() {
        return new Queue(STOCK_QUEUE, true);
    }

    @Bean
    public TopicExchange warehouseExchange() {
        return new TopicExchange(WAREHOUSE_EVENTS_EXCHANGE);
    }

    @Bean
    public Binding stockBinding(Queue stockQueue, TopicExchange warehouseExchange) {
        return BindingBuilder.bind(stockQueue).to(warehouseExchange).with("stock.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
