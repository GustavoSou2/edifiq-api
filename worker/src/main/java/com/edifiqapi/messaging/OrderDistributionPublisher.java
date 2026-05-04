package com.edifiqapi.messaging;

import com.edifiqapi.domain.order.OrderDistribution;
import com.edifiqapi.repository.order.OrderDistributionRepository;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OrderDistributionPublisher {
    private final OrderDistributionRepository orderDistributionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange orderDistributionExchange;
    private final String routingKey;

    public OrderDistributionPublisher(
            OrderDistributionRepository orderDistributionRepository,
            RabbitTemplate rabbitTemplate,
            @Qualifier("orderDistributionExchange") TopicExchange orderDistributionExchange,
            @Value("${edifiq.messaging.order-distribution-routing-key}") String routingKey
    ) {
        this.orderDistributionRepository = orderDistributionRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.orderDistributionExchange = orderDistributionExchange;
        this.routingKey = routingKey;
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderDistributionsQueued(OrderDistributionsQueuedEvent event) {
        List<OrderDistribution> distributions = orderDistributionRepository.findAllById(event.distributionIds());
        for (OrderDistribution distribution : distributions) {
            String messageId = UUID.randomUUID().toString();
            OrderDistributionDispatchMessage message = new OrderDistributionDispatchMessage(
                    distribution.getId(),
                    distribution.getOrder().getId(),
                    distribution.getSupplier().getId(),
                    distribution.getSupplier().getName(),
                    distribution.getSupplier().getEmail(),
                    distribution.getSupplier().getPhone()
            );

            rabbitTemplate.convertAndSend(orderDistributionExchange.getName(), routingKey, message, rabbitMessage -> {
                rabbitMessage.getMessageProperties().setMessageId(messageId);
                return rabbitMessage;
            });

            distribution.setStatus(OrderDistribution.Status.queued);
            distribution.setQueueMessageId(messageId);
            distribution.setQueuedAt(Instant.now());
            distribution.setFailureReason(null);
            distribution.setFailedAt(null);
            orderDistributionRepository.save(distribution);
        }
    }
}
