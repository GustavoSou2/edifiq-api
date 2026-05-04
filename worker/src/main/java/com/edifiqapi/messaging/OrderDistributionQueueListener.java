package com.edifiqapi.messaging;

import com.edifiqapi.domain.order.OrderDistribution;
import com.edifiqapi.repository.order.OrderDistributionRepository;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Component
public class OrderDistributionQueueListener {
    private final OrderDistributionRepository orderDistributionRepository;

    public OrderDistributionQueueListener(OrderDistributionRepository orderDistributionRepository) {
        this.orderDistributionRepository = orderDistributionRepository;
    }

    @Transactional
    @RabbitListener(queues = "#{@orderDistributionQueue.name}")
    public void handle(OrderDistributionDispatchMessage message) {
        OrderDistribution distribution = orderDistributionRepository.findById(message.distributionId())
                .orElseThrow(() -> new AmqpRejectAndDontRequeueException("distribution not found: " + message.distributionId()));

        distribution.setStatus(OrderDistribution.Status.processing);
        distribution.setProcessingAt(Instant.now());
        distribution.setDispatchAttempts(distribution.getDispatchAttempts() + 1);
        orderDistributionRepository.save(distribution);

        try {
            if (!distribution.getSupplier().isActive()) {
                throw new IllegalStateException("supplier is inactive");
            }

            boolean hasDispatchChannel = StringUtils.hasText(distribution.getSupplier().getEmail())
                    || StringUtils.hasText(distribution.getSupplier().getPhone());
            if (!hasDispatchChannel) {
                throw new IllegalStateException("supplier has no dispatch channel");
            }

            distribution.setStatus(OrderDistribution.Status.sent);
            distribution.setSentAt(Instant.now());
            distribution.setFailureReason(null);
            distribution.setFailedAt(null);
            orderDistributionRepository.save(distribution);
        } catch (RuntimeException ex) {
            distribution.setStatus(OrderDistribution.Status.failed);
            distribution.setFailedAt(Instant.now());
            distribution.setFailureReason(ex.getMessage());
            orderDistributionRepository.save(distribution);
            throw new AmqpRejectAndDontRequeueException("distribution dispatch failed: " + distribution.getId(), ex);
        }
    }
}
