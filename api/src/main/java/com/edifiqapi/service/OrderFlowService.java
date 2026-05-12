package com.edifiqapi.service;

import com.edifiqapi.domain.delivery.Delivery;
import com.edifiqapi.domain.delivery.Rating;
import com.edifiqapi.domain.order.Order;
import com.edifiqapi.domain.order.OrderDistribution;
import com.edifiqapi.domain.order.OrderItem;
import com.edifiqapi.domain.order.OrderSelection;
import com.edifiqapi.domain.supplier.SupplierScoreDTO;
import com.edifiqapi.messaging.OrderDistributionsQueuedEvent;
import com.edifiqapi.domain.proposal.Proposal;
import com.edifiqapi.domain.proposal.ProposalItem;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.supplier.Supplier;
import com.edifiqapi.domain.tenant.Tenant;
import com.edifiqapi.repository.catalog.CategoryRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class OrderFlowService {
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDistributionRepository orderDistributionRepository;
    private final ProposalRepository proposalRepository;
    private final ProposalItemRepository proposalItemRepository;
    private final OrderSelectionRepository orderSelectionRepository;
    private final DeliveryRepository deliveryRepository;
    private final RatingRepository ratingRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final Logger log = LoggerFactory.getLogger(OrderFlowService.class);

    public OrderFlowService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            SupplierRepository supplierRepository,
            CategoryRepository categoryRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderDistributionRepository orderDistributionRepository,
            ProposalRepository proposalRepository,
            ProposalItemRepository proposalItemRepository,
            OrderSelectionRepository orderSelectionRepository,
            DeliveryRepository deliveryRepository,
            RatingRepository ratingRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderDistributionRepository = orderDistributionRepository;
        this.proposalRepository = proposalRepository;
        this.proposalItemRepository = proposalItemRepository;
        this.orderSelectionRepository = orderSelectionRepository;
        this.deliveryRepository = deliveryRepository;
        this.ratingRepository = ratingRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Order createOrder(
            String tenantId,
            String userId,
            String title,
            String deliveryAddress,
            String deliveryCity,
            String deliveryState,
            Double deliveryLat,
            Double deliveryLng,
            Instant deliveryWindowStart,
            Instant deliveryWindowEnd,
            boolean isUrgent,
            int auctionDurationMin,
            int maxSuppliers,
            String notes,
            String referenceCode,
            List<CreateOrderItem> items
    ) {
        // Valida que o tenant existe
        tenantRepository.findById(tenantId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "tenant not found"));
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        // Valida que o usuário existe e pertence ao tenant — retorna entidade gerenciada na sessão atual
        if (userId == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "missing user id in token");
        }

        userRepository.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found: " + userId));
        User user = userRepository.getReferenceById(userId);

        Order order = new Order();
        order.setTenant(tenant);
        order.setCreatedBy(user);
        order.setTitle(title);
        order.setDeliveryAddress(deliveryAddress);
        order.setDeliveryCity(deliveryCity);
        order.setDeliveryState(deliveryState);
        order.setDeliveryLat(deliveryLat);
        order.setDeliveryLng(deliveryLng);
        order.setDeliveryWindowStart(deliveryWindowStart);
        order.setDeliveryWindowEnd(deliveryWindowEnd);
        order.setUrgent(isUrgent);
        order.setAuctionDurationMin(auctionDurationMin);
        order.setMaxSuppliers(maxSuppliers);
        order.setNotes(notes);
        order.setReferenceCode(referenceCode);
        order.setStatus(Order.Status.draft);

        System.out.println(">>> PRE-SAVE tenant=" +
                (order.getTenant() != null ? order.getTenant().getId() : "NULL") +
                " createdBy=" +
                (order.getCreatedBy() != null ? order.getCreatedBy().getId() : "NULL") +
                " userId=" + userId
        );

        order = orderRepository.save(order);


        for (CreateOrderItem item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setDescription(item.description());
            orderItem.setUnit(item.unit());
            orderItem.setQuantity(item.quantity());
            orderItem.setNotes(item.notes());
            orderItem.setSortOrder(item.sortOrder() != null ? item.sortOrder() : 0);

            if (item.categoryId() != null) {
                orderItem.setCategory(categoryRepository.getReferenceById(item.categoryId()));
            }

            orderItemRepository.save(orderItem);
        }

        return order;
    }

    @Transactional
    public Order updateOrder(String tenantId, String orderId, UpdateOrderRequest request) {
        Order order = orderRepository.findByIdAndTenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "order not found"));

        if (request.deliveryAddress() != null) order.setDeliveryAddress(request.deliveryAddress());
        if (request.deliveryCity()    != null) order.setDeliveryCity(request.deliveryCity());
        if (request.deliveryState()   != null) {
            // delivery_state é CHAR(2) no banco — garante que nunca excede 2 chars
            String state = request.deliveryState().trim();
            order.setDeliveryState(state.length() > 2 ? state.substring(0, 2).toUpperCase() : state.toUpperCase());
        }
        if (request.deliveryLat()     != null) order.setDeliveryLat(request.deliveryLat());
        if (request.deliveryLng()     != null) order.setDeliveryLng(request.deliveryLng());
        if (request.title()           != null) order.setTitle(request.title());
        if (request.notes()           != null) order.setNotes(request.notes());

        return orderRepository.save(order);
    }

    public record UpdateOrderRequest(
            String deliveryAddress,
            String deliveryCity,
            String deliveryState,
            Double deliveryLat,
            Double deliveryLng,
            String title,
            String notes
    ) {}

    @Transactional
    public List<OrderDistribution> publishAndDistribute(String tenantId, String orderId) {

        Order order = orderRepository.findByIdAndTenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "order not found"));

        if (order.getStatus() != Order.Status.draft) {
            throw new ResponseStatusException(BAD_REQUEST, "order must be draft to publish");
        }

        order.setStatus(Order.Status.open);
        orderRepository.save(order);

        List<Supplier> suppliers = supplierRepository.findAllByTenant_Id(tenantId).stream()
                .filter(Supplier::isActive)
                .toList();

        List<Supplier> topSuppliers = rankSuppliers(order, suppliers);

        List<OrderDistribution> distributions = new ArrayList<>();

        for (Supplier supplier : topSuppliers) {
            OrderDistribution distribution = new OrderDistribution();
            distribution.setOrder(order);
            distribution.setSupplier(supplier);
            distribution.setStatus(OrderDistribution.Status.pending);
            distributions.add(orderDistributionRepository.save(distribution));
        }

        applicationEventPublisher.publishEvent(new OrderDistributionsQueuedEvent(
                distributions.stream().map(OrderDistribution::getId).toList()
        ));

        return distributions;
    }

    @Transactional
    public Proposal submitProposal(String tenantId, String distributionId, Proposal.Status status, Integer deliveryEtaHours, Instant proposedDeliveryAt, String message, List<CreateProposalItem> items) {
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
        proposal.setProposedDeliveryAt(proposedDeliveryAt);
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
    public OrderSelection selectProposal(String tenantId, String userId, String orderId, String proposalId) {
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

        userRepository.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        User selectedBy = userRepository.getReferenceById(userId);

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

        order.setStatus(Order.Status.selected);
        orderRepository.save(order);

        return selection;
    }

    @Transactional
    public Delivery updateDeliveryStatus(String tenantId, String deliveryId, Delivery.Status status, String trackingCode, String proofUrl) {
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
    public Rating createRating(String tenantId, String userId, String selectionId, int score, String comment) {
        if (score < 1 || score > 5) {
            throw new ResponseStatusException(BAD_REQUEST, "score must be between 1 and 5");
        }

        OrderSelection selection = orderSelectionRepository.findByIdAndOrder_Tenant_Id(selectionId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "selection not found"));

        User ratedBy = userRepository.findByIdAndTenant_Id(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        User ratedByRef = userRepository.getReferenceById(userId);

        Supplier supplier = selection.getProposal().getOrderDistribution().getSupplier();

        Rating rating = new Rating();
        rating.setOrderSelection(selection);
        rating.setSupplier(supplier);
        rating.setRatedBy(ratedByRef);
        rating.setScore(score);
        rating.setComment(comment);
        return ratingRepository.save(rating);
    }

    public record CreateOrderItem(String categoryId, String description, String unit, BigDecimal quantity, String notes, Integer sortOrder) {}

    public record CreateProposalItem(
            String orderItemId,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            ProposalItem.Availability availability
    ) {}


    private double normalize(double value, double min, double max) {
        if (max - min == 0) return 1;
        return (value - min) / (max - min);
    }

    private double invertNormalize(double value, double min, double max) {
        if (max - min == 0) return 1;
        return 1 - ((value - min) / (max - min));
    }


    private List<Supplier> rankSuppliers(Order order, List<Supplier> suppliers) {

        Map<String, Double> preco = new HashMap<>();
        Map<String, Double> prazo = new HashMap<>();
        Map<String, Double> resposta = new HashMap<>();
        Map<String, Double> reputacao = new HashMap<>();
        Map<String, Double> distancia = new HashMap<>();

        double minPreco = preco.values().stream().min(Double::compare).orElse(1.0);
        double maxPreco = preco.values().stream().max(Double::compare).orElse(1.0);

        double minPrazo = prazo.values().stream().min(Double::compare).orElse(1.0);
        double maxPrazo = prazo.values().stream().max(Double::compare).orElse(1.0);

        double minDist = distancia.values().stream().min(Double::compare).orElse(1.0);
        double maxDist = distancia.values().stream().max(Double::compare).orElse(1.0);

        List<SupplierScoreDTO> scores = new ArrayList<>();

        for (Supplier s : suppliers) {

            double precoNorm = invertNormalize(preco.get(s.getId()), minPreco, maxPreco);
            double prazoNorm = invertNormalize(prazo.get(s.getId()), minPrazo, maxPrazo);
            double respostaNorm = normalize(resposta.get(s.getId()), 0, 1);
            double reputacaoNorm = normalize(reputacao.get(s.getId()), 0, 5);
            double proximidadeNorm = invertNormalize(distancia.get(s.getId()), minDist, maxDist);

            double scoreFinal =
                    (precoNorm * 0.30) +
                            (prazoNorm * 0.25) +
                            (respostaNorm * 0.20) +
                            (reputacaoNorm * 0.15) +
                            (proximidadeNorm * 0.10);

            scores.add(new SupplierScoreDTO(s, scoreFinal));
        }

        return scores.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(5)
                .map(SupplierScoreDTO::getSupplier)
                .toList();
    }
}



