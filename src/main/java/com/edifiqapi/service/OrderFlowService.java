package com.edifiqapi.service;

import com.edifiqapi.domain.delivery.Delivery;
import com.edifiqapi.domain.delivery.Rating;
import com.edifiqapi.domain.order.Order;
import com.edifiqapi.domain.order.OrderDistribution;
import com.edifiqapi.domain.order.OrderItem;
import com.edifiqapi.domain.order.OrderSelection;
import com.edifiqapi.domain.proposal.Proposal;
import com.edifiqapi.domain.proposal.ProposalItem;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.supplier.Supplier;
import com.edifiqapi.domain.tenant.Tenant;
import com.edifiqapi.repository.delivery.DeliveryRepository;
import com.edifiqapi.repository.delivery.RatingRepository;
import com.edifiqapi.repository.order.OrderDistributionRepository;
import com.edifiqapi.repository.order.OrderItemRepository;
import com.edifiqapi.repository.order.OrderRepository;
import com.edifiqapi.repository.order.OrderSelectionRepository;
import com.edifiqapi.repository.proposal.ProposalItemRepository;
import com.edifiqapi.repository.proposal.ProposalRepository;
import com.edifiqapi.repository.rbac.UserRepository;
import com.edifiqapi.repository.supplier.SupplierRepository;
import com.edifiqapi.repository.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class OrderFlowService {
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDistributionRepository orderDistributionRepository;
    private final ProposalRepository proposalRepository;
    private final ProposalItemRepository proposalItemRepository;
    private final OrderSelectionRepository orderSelectionRepository;
    private final DeliveryRepository deliveryRepository;
    private final RatingRepository ratingRepository;

    public OrderFlowService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            SupplierRepository supplierRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderDistributionRepository orderDistributionRepository,
            ProposalRepository proposalRepository,
            ProposalItemRepository proposalItemRepository,
            OrderSelectionRepository orderSelectionRepository,
            DeliveryRepository deliveryRepository,
            RatingRepository ratingRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderDistributionRepository = orderDistributionRepository;
        this.proposalRepository = proposalRepository;
        this.proposalItemRepository = proposalItemRepository;
        this.orderSelectionRepository = orderSelectionRepository;
        this.deliveryRepository = deliveryRepository;
        this.ratingRepository = ratingRepository;
    }

    @Transactional
    public Order createOrder(long tenantId, long userId, String title, String description, Instant scheduledAt, List<CreateOrderItem> items) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "tenant not found"));
        User user = userRepository.findByIdAndTenant_Id(userId, tenantId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        Order order = new Order();
        order.setTenant(tenant);
        order.setCreatedBy(user);
        order.setTitle(title);
        order.setDescription(description);
        order.setStatus(Order.Status.draft);
        order.setScheduledAt(scheduledAt);
        order = orderRepository.save(order);

        for (CreateOrderItem item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setName(item.name());
            orderItem.setUnit(item.unit());
            orderItem.setQuantity(item.quantity());
            orderItem.setNotes(item.notes());
            orderItemRepository.save(orderItem);
        }

        return order;
    }

    @Transactional
    public List<OrderDistribution> publishAndDistribute(long tenantId, long orderId) {
        Order order = orderRepository.findByIdAndTenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "order not found"));

        if (order.getStatus() != Order.Status.draft) {
            throw new ResponseStatusException(BAD_REQUEST, "order must be draft to publish");
        }

        order.setStatus(Order.Status.published);
        orderRepository.save(order);

        List<Supplier> suppliers = supplierRepository.findAllByTenant_Id(tenantId).stream()
                .filter(Supplier::isActive)
                .toList();

        List<OrderDistribution> distributions = new ArrayList<>();
        for (Supplier supplier : suppliers) {
            OrderDistribution distribution = new OrderDistribution();
            distribution.setOrder(order);
            distribution.setSupplier(supplier);
            distribution.setStatus(OrderDistribution.Status.pending);
            distributions.add(orderDistributionRepository.save(distribution));
        }

        return distributions;
    }

    @Transactional
    public Proposal submitProposal(long tenantId, long distributionId, Proposal.Status status, Integer deliveryEtaHours, String message, List<CreateProposalItem> items) {
        OrderDistribution distribution = orderDistributionRepository.findByIdAndOrder_Tenant_Id(distributionId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "distribution not found"));

        BigDecimal total = BigDecimal.ZERO;
        for (CreateProposalItem item : items) {
            total = total.add(item.totalPrice());
        }

        Proposal proposal = new Proposal();
        proposal.setOrderDistribution(distribution);
        proposal.setStatus(status);
        proposal.setDeliveryEtaHours(deliveryEtaHours);
        proposal.setMessage(message);
        proposal.setTotalPrice(total);
        proposal = proposalRepository.save(proposal);

        for (CreateProposalItem item : items) {
            OrderItem orderItem = orderItemRepository.findByIdAndOrder_Tenant_Id(item.orderItemId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "order item not found: " + item.orderItemId()));

            ProposalItem proposalItem = new ProposalItem();
            proposalItem.setProposal(proposal);
            proposalItem.setOrderItem(orderItem);
            proposalItem.setUnitPrice(item.unitPrice());
            proposalItem.setTotalPrice(item.totalPrice());
            proposalItem.setAvailability(item.availability());
            proposalItemRepository.save(proposalItem);
        }

        return proposal;
    }

    @Transactional
    public OrderSelection selectProposal(long tenantId, long userId, long orderId, long proposalId) {
        Order order = orderRepository.findByIdAndTenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "order not found"));

        Proposal proposal = proposalRepository.findByIdAndOrderDistribution_Order_Tenant_Id(proposalId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "proposal not found"));

        if (!proposal.getOrderDistribution().getOrder().getId().equals(order.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "proposal does not belong to order");
        }

        if (orderSelectionRepository.findByOrder_IdAndOrder_Tenant_Id(orderId, tenantId).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "order already selected");
        }

        User selectedBy = userRepository.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        OrderSelection selection = new OrderSelection();
        selection.setOrder(order);
        selection.setProposal(proposal);
        selection.setSelectedBy(selectedBy);
        selection.setStatus(OrderSelection.Status.selected);
        selection = orderSelectionRepository.save(selection);

        Delivery delivery = new Delivery();
        delivery.setOrderSelection(selection);
        delivery.setStatus(Delivery.Status.scheduled);
        deliveryRepository.save(delivery);

        order.setStatus(Order.Status.closed);
        orderRepository.save(order);

        return selection;
    }

    @Transactional
    public Delivery updateDeliveryStatus(long tenantId, long deliveryId, Delivery.Status status, String trackingCode, String proofUrl) {
        Delivery delivery = deliveryRepository.findByIdAndOrderSelection_Order_Tenant_Id(deliveryId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "delivery not found"));

        delivery.setStatus(status);
        if (trackingCode != null) {
            delivery.setTrackingCode(trackingCode);
        }
        if (proofUrl != null) {
            delivery.setProofUrl(proofUrl);
        }
        if (status == Delivery.Status.in_transit && delivery.getDispatchedAt() == null) {
            delivery.setDispatchedAt(Instant.now());
        }
        if (status == Delivery.Status.delivered && delivery.getDeliveredAt() == null) {
            delivery.setDeliveredAt(Instant.now());
        }
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public Rating createRating(long tenantId, long userId, long selectionId, int score, String comment) {
        if (score < 1 || score > 5) {
            throw new ResponseStatusException(BAD_REQUEST, "score must be between 1 and 5");
        }

        OrderSelection selection = orderSelectionRepository.findByIdAndOrder_Tenant_Id(selectionId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "selection not found"));

        User ratedBy = userRepository.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));

        Supplier supplier = selection.getProposal().getOrderDistribution().getSupplier();

        Rating rating = new Rating();
        rating.setOrderSelection(selection);
        rating.setSupplier(supplier);
        rating.setRatedBy(ratedBy);
        rating.setScore(score);
        rating.setComment(comment);
        return ratingRepository.save(rating);
    }

    public record CreateOrderItem(String name, String unit, BigDecimal quantity, String notes) {}

    public record CreateProposalItem(
            Long orderItemId,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            ProposalItem.Availability availability
    ) {}
}

