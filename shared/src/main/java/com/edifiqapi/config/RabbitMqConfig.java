package com.edifiqapi.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMqConfig {
    @Bean
    TopicExchange orderDistributionExchange(
            @Value("${edifiq.messaging.order-distribution-exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    TopicExchange orderDistributionDeadLetterExchange(
            @Value("${edifiq.messaging.order-distribution-dlx}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue orderDistributionQueue(
            @Value("${edifiq.messaging.order-distribution-queue}") String queueName,
            @Value("${edifiq.messaging.order-distribution-dlx}") String deadLetterExchange,
            @Value("${edifiq.messaging.order-distribution-dlq-routing-key}") String deadLetterRoutingKey
    ) {
        return new Queue(queueName, true, false, false, Map.of(
                "x-dead-letter-exchange", deadLetterExchange,
                "x-dead-letter-routing-key", deadLetterRoutingKey
        ));
    }

    @Bean
    Queue orderDistributionDeadLetterQueue(
            @Value("${edifiq.messaging.order-distribution-dlq}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding orderDistributionBinding(
            @Qualifier("orderDistributionQueue") Queue orderDistributionQueue,
            @Qualifier("orderDistributionExchange") TopicExchange orderDistributionExchange,
            @Value("${edifiq.messaging.order-distribution-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(orderDistributionQueue).to(orderDistributionExchange).with(routingKey);
    }

    @Bean
    Binding orderDistributionDeadLetterBinding(
            @Qualifier("orderDistributionDeadLetterQueue") Queue orderDistributionDeadLetterQueue,
            @Qualifier("orderDistributionDeadLetterExchange") TopicExchange orderDistributionDeadLetterExchange,
            @Value("${edifiq.messaging.order-distribution-dlq-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(orderDistributionDeadLetterQueue).to(orderDistributionDeadLetterExchange).with(routingKey);
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
