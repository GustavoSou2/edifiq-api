package com.edifiqapi.controller;

import com.edifiqapi.domain.delivery.Delivery;
import com.edifiqapi.domain.delivery.Rating;
import com.edifiqapi.domain.order.Order;
import com.edifiqapi.domain.order.OrderDistribution;
import com.edifiqapi.domain.order.OrderItem;
import com.edifiqapi.domain.order.OrderSelection;
import com.edifiqapi.domain.proposal.Proposal;
import com.edifiqapi.domain.proposal.ProposalItem;
import com.edifiqapi.repository.delivery.DeliveryRepository;
import com.edifiqapi.repository.order.OrderDistributionRepository;
import com.edifiqapi.repository.order.OrderItemRepository;
import com.edifiqapi.repository.order.OrderRepository;
import com.edifiqapi.repository.order.OrderSelectionRepository;
import com.edifiqapi.repository.proposal.ProposalItemRepository;
import com.edifiqapi.repository.proposal.ProposalRepository;
import com.edifiqapi.security.JwtClaims;
import com.edifiqapi.service.OrderFlowService;
import com.edifiqapi.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDistributionRepository orderDistributionRepository;
    private final ProposalRepository proposalRepository;
    private final ProposalItemRepository proposalItemRepository;
    private final OrderSelectionRepository orderSelectionRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderFlowService orderFlowService;

    public OrderController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderDistributionRepository orderDistributionRepository,
            ProposalRepository proposalRepository,
            ProposalItemRepository proposalItemRepository,
            OrderSelectionRepository orderSelectionRepository,
            DeliveryRepository deliveryRepository,
            OrderFlowService orderFlowService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderDistributionRepository = orderDistributionRepository;
        this.proposalRepository = proposalRepository;
        this.proposalItemRepository = proposalItemRepository;
        this.orderSelectionRepository = orderSelectionRepository;
        this.deliveryRepository = deliveryRepository;
        this.orderFlowService = orderFlowService;
    }

    @GetMapping
    public ApiResponse<List<OrderSummaryResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(orderRepository.findAllByTenant_Id(tenantId).stream().map(OrderSummaryResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailsResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        Order order = orderRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "order not found"));
        List<OrderItemResponse> items = orderItemRepository.findAllByOrder_Id(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();
        return ApiResponse.of(OrderDetailsResponse.from(order, items));
    }

    @PostMapping
    public ApiResponse<OrderDetailsResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrderRequest request) {
        String tenantId = JwtClaims.tenantId(jwt);
        String userId = JwtClaims.userId(jwt);
        Order order = orderFlowService.createOrder(
                tenantId,
                userId,
                request.title(),
                request.deliveryAddress(),
                request.deliveryCity(),
                request.deliveryState(),
                request.deliveryLat(),
                request.deliveryLng(),
                request.deliveryWindowStart(),
                request.deliveryWindowEnd(),
                request.isUrgent(),
                request.auctionDurationMin() != null ? request.auctionDurationMin() : 60,
                request.maxSuppliers() != null ? request.maxSuppliers() : 10,
                request.notes(),
                request.referenceCode(),
                request.items().stream()
                        .map(i -> new OrderFlowService.CreateOrderItem(i.categoryId(), i.description(), i.unit(), i.quantity(), i.notes(), i.sortOrder()))
                        .toList()
        );
        List<OrderItemResponse> items = orderItemRepository.findAllByOrder_Id(order.getId()).stream().map(OrderItemResponse::from).toList();
        return ApiResponse.of(OrderDetailsResponse.from(order, items));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<List<OrderDistributionResponse>> publish(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        List<OrderDistribution> distributions = orderFlowService.publishAndDistribute(tenantId, id);
        return ApiResponse.of(distributions.stream().map(OrderDistributionResponse::from).toList());
    }

    @PutMapping("/{id}")
    public ApiResponse<OrderDetailsResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody UpdateOrderRequest request
    ) {
        String tenantId = JwtClaims.tenantId(jwt);
        Order order = orderFlowService.updateOrder(tenantId, id,
                new OrderFlowService.UpdateOrderRequest(
                        request.deliveryAddress(),
                        request.deliveryCity(),
                        request.deliveryState(),
                        request.deliveryLat(),
                        request.deliveryLng(),
                        request.title(),
                        request.notes()
                ));
        List<OrderItemResponse> items = orderItemRepository.findAllByOrder_Id(order.getId())
                .stream().map(OrderItemResponse::from).toList();
        return ApiResponse.of(OrderDetailsResponse.from(order, items));
    }

    @GetMapping("/{id}/distributions")
    public ApiResponse<List<OrderDistributionResponse>> distributions(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(orderDistributionRepository.findAllByOrder_IdAndOrder_Tenant_Id(id, tenantId).stream()
                .map(OrderDistributionResponse::from).toList());
    }

    @GetMapping("/{id}/proposals")
    public ApiResponse<List<ProposalResponse>> proposals(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String tenantId = JwtClaims.tenantId(jwt);
        return ApiResponse.of(proposalRepository.findAllByOrderDistribution_Order_IdAndOrderDistribution_Order_Tenant_Id(id, tenantId)
                .stream().map(ProposalResponse::from).toList());
    }

    @GetMapping("/{orderId}/selection")
    public ApiResponse<OrderSelectionResponse> selection(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderId) {
        String tenantId = JwtClaims.tenantId(jwt);
        OrderSelection selection = orderSelectionRepository.findByOrder_IdAndOrder_Tenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "selection not found"));
        return ApiResponse.of(OrderSelectionResponse.from(selection));
    }

    @PostMapping("/{orderId}/select")
    public ApiResponse<OrderSelectionResponse> select(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @Valid @RequestBody SelectProposalRequest request
    ) {
        String tenantId = JwtClaims.tenantId(jwt);
        String userId = JwtClaims.userId(jwt);
        OrderSelection selection = orderFlowService.selectProposal(tenantId, userId, orderId, request.proposalId());
        return ApiResponse.of(OrderSelectionResponse.from(selection));
    }

    @GetMapping("/{orderId}/delivery")
    public ApiResponse<DeliveryResponse> delivery(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderId) {
        String tenantId = JwtClaims.tenantId(jwt);
        OrderSelection selection = orderSelectionRepository.findByOrder_IdAndOrder_Tenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "selection not found"));
        Delivery delivery = deliveryRepository.findByOrderSelection_IdAndOrderSelection_Order_Tenant_Id(selection.getId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "delivery not found"));
        return ApiResponse.of(DeliveryResponse.from(delivery));
    }

    @PostMapping("/{orderId}/rate")
    public ApiResponse<RatingResponse> rate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @Valid @RequestBody CreateRatingRequest request
    ) {
        String tenantId = JwtClaims.tenantId(jwt);
        String userId = JwtClaims.userId(jwt);
        OrderSelection selection = orderSelectionRepository.findByOrder_IdAndOrder_Tenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "selection not found"));
        Rating rating = orderFlowService.createRating(tenantId, userId, selection.getId(), request.score(), request.comment());
        return ApiResponse.of(RatingResponse.from(rating));
    }

    @GetMapping("/{orderId}/proposals/{proposalId}/items")
    public ApiResponse<List<ProposalItemResponse>> proposalItems(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId,
            @PathVariable String proposalId
    ) {
        String tenantId = JwtClaims.tenantId(jwt);
        orderRepository.findByIdAndTenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "order not found"));
        proposalRepository.findByIdAndOrderDistribution_Order_Tenant_Id(proposalId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "proposal not found"));
        return ApiResponse.of(proposalItemRepository.findAllByProposal_Id(proposalId).stream().map(ProposalItemResponse::from).toList());
    }

    // ── Request records ──────────────────────────────────────────────────────

    public record UpdateOrderRequest(
            String deliveryAddress,
            String deliveryCity,
            String deliveryState,
            Double deliveryLat,
            Double deliveryLng,
            String title,
            String notes
    ) {}

    public record CreateOrderRequest(
            String title,
            String deliveryAddress,
            String deliveryCity,
            String deliveryState,
            Double deliveryLat,
            Double deliveryLng,
            @NotNull Instant deliveryWindowStart,
            Instant deliveryWindowEnd,
            boolean isUrgent,
            Integer auctionDurationMin,
            Integer maxSuppliers,
            String notes,
            String referenceCode,
            @NotNull List<CreateOrderItemRequest> items
    ) {}

    public record CreateOrderItemRequest(
            String categoryId,
            @NotBlank String description,
            String unit,
            @NotNull BigDecimal quantity,
            String notes,
            Integer sortOrder
    ) {}

    public record SelectProposalRequest(@NotNull String proposalId) {}

    public record CreateRatingRequest(@NotNull Integer score, String comment) {}

    // ── Response records ─────────────────────────────────────────────────────

    public record OrderSummaryResponse(String id, String title, String referenceCode, Order.Status status, boolean isUrgent, String deliveryCity, String deliveryState, Instant createdAt) {
        static OrderSummaryResponse from(Order order) {
            return new OrderSummaryResponse(order.getId(), order.getTitle(), order.getReferenceCode(), order.getStatus(), order.isUrgent(), order.getDeliveryCity(), order.getDeliveryState(), order.getCreatedAt());
        }
    }

    public record OrderDetailsResponse(
            String id,
            String title,
            String referenceCode,
            String notes,
            Order.Status status,
            boolean isUrgent,
            String deliveryAddress,
            String deliveryCity,
            String deliveryState,
            Double deliveryLat,
            Double deliveryLng,
            Instant deliveryWindowStart,
            Instant deliveryWindowEnd,
            int maxSuppliers,
            int auctionDurationMin,
            Instant createdAt,
            List<OrderItemResponse> items
    ) {
        static OrderDetailsResponse from(Order order, List<OrderItemResponse> items) {
            return new OrderDetailsResponse(
                    order.getId(),
                    order.getTitle(),
                    order.getReferenceCode(),
                    order.getNotes(),
                    order.getStatus(),
                    order.isUrgent(),
                    order.getDeliveryAddress(),
                    order.getDeliveryCity(),
                    order.getDeliveryState(),
                    order.getDeliveryLat(),
                    order.getDeliveryLng(),
                    order.getDeliveryWindowStart(),
                    order.getDeliveryWindowEnd(),
                    order.getMaxSuppliers(),
                    order.getAuctionDurationMin(),
                    order.getCreatedAt(),
                    items
            );
        }
    }

    public record OrderItemResponse(String id, String categoryId, String description, String unit, BigDecimal quantity, String notes, int sortOrder) {
        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getCategory() != null ? item.getCategory().getId() : null,
                    item.getDescription(),
                    item.getUnit(),
                    item.getQuantity(),
                    item.getNotes(),
                    item.getSortOrder()
            );
        }
    }

    public record OrderDistributionResponse(
            String id,
            String orderId,
            String supplierId,
            OrderDistribution.Status status,
            Instant distributedAt,
            String queueMessageId,
            Instant queuedAt,
            Instant processingAt,
            Instant sentAt,
            Instant failedAt,
            Integer dispatchAttempts,
            String failureReason
    ) {
        static OrderDistributionResponse from(OrderDistribution distribution) {
            return new OrderDistributionResponse(
                    distribution.getId(),
                    distribution.getOrder().getId(),
                    distribution.getSupplier().getId(),
                    distribution.getStatus(),
                    distribution.getDistributedAt(),
                    distribution.getQueueMessageId(),
                    distribution.getQueuedAt(),
                    distribution.getProcessingAt(),
                    distribution.getSentAt(),
                    distribution.getFailedAt(),
                    distribution.getDispatchAttempts(),
                    distribution.getFailureReason()
            );
        }
    }

    public record ProposalResponse(String id, String distributionId, Proposal.Status status, BigDecimal totalPrice, Integer deliveryEtaHours, Instant proposedDeliveryAt, String message) {
        static ProposalResponse from(Proposal proposal) {
            return new ProposalResponse(
                    proposal.getId(),
                    proposal.getOrderDistribution().getId(),
                    proposal.getStatus(),
                    proposal.getTotalPrice(),
                    proposal.getDeliveryEtaHours(),
                    proposal.getProposedDeliveryAt(),
                    proposal.getMessage()
            );
        }
    }

    public record ProposalItemResponse(String id, String orderItemId, BigDecimal unitPrice, BigDecimal totalPrice, ProposalItem.Availability availability) {
        static ProposalItemResponse from(ProposalItem item) {
            return new ProposalItemResponse(item.getId(), item.getOrderItem().getId(), item.getUnitPrice(), item.getTotalPrice(), item.getAvailability());
        }
    }

    public record OrderSelectionResponse(String id, String orderId, String proposalId, String selectedBy, OrderSelection.Status status, Instant selectedAt) {
        static OrderSelectionResponse from(OrderSelection selection) {
            return new OrderSelectionResponse(
                    selection.getId(),
                    selection.getOrder().getId(),
                    selection.getProposal().getId(),
                    selection.getSelectedBy().getId(),
                    selection.getStatus(),
                    selection.getSelectedAt()
            );
        }
    }

    public record DeliveryResponse(String id, String selectionId, Delivery.Status status, String trackingCode, Instant scheduledAt, Instant dispatchedAt, Instant deliveredAt, String proofUrl) {
        static DeliveryResponse from(Delivery delivery) {
            return new DeliveryResponse(
                    delivery.getId(),
                    delivery.getOrderSelection().getId(),
                    delivery.getStatus(),
                    delivery.getTrackingCode(),
                    delivery.getScheduledAt(),
                    delivery.getDispatchedAt(),
                    delivery.getDeliveredAt(),
                    delivery.getProofUrl()
            );
        }
    }

    public record RatingResponse(String id, String selectionId, String supplierId, int score, String comment, String response) {
        static RatingResponse from(Rating rating) {
            return new RatingResponse(
                    rating.getId(),
                    rating.getOrderSelection().getId(),
                    rating.getSupplier().getId(),
                    rating.getScore(),
                    rating.getComment(),
                    rating.getResponse()
            );
        }
    }
}
