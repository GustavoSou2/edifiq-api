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
    public List<OrderSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        long tenantId = JwtClaims.tenantId(jwt);
        return orderRepository.findAllByTenant_Id(tenantId).stream().map(OrderSummaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderDetailsResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        long tenantId = JwtClaims.tenantId(jwt);
        Order order = orderRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "order not found"));
        List<OrderItemResponse> items = orderItemRepository.findAllByOrder_Id(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList();
        return OrderDetailsResponse.from(order, items);
    }

    @PostMapping
    public OrderDetailsResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrderRequest request) {
        long tenantId = JwtClaims.tenantId(jwt);
        long userId = JwtClaims.userId(jwt);
        Order order = orderFlowService.createOrder(
                tenantId,
                userId,
                request.title(),
                request.description(),
                request.scheduledAt(),
                request.items().stream()
                        .map(i -> new OrderFlowService.CreateOrderItem(i.name(), i.unit(), i.quantity(), i.notes()))
                        .toList()
        );
        List<OrderItemResponse> items = orderItemRepository.findAllByOrder_Id(order.getId()).stream().map(OrderItemResponse::from).toList();
        return OrderDetailsResponse.from(order, items);
    }

    @PostMapping("/{id}/publish")
    public List<OrderDistributionResponse> publish(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        long tenantId = JwtClaims.tenantId(jwt);
        List<OrderDistribution> distributions = orderFlowService.publishAndDistribute(tenantId, id);
        return distributions.stream().map(OrderDistributionResponse::from).toList();
    }

    @GetMapping("/{id}/distributions")
    public List<OrderDistributionResponse> distributions(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        long tenantId = JwtClaims.tenantId(jwt);
        return orderDistributionRepository.findAllByOrder_IdAndOrder_Tenant_Id(id, tenantId).stream()
                .map(OrderDistributionResponse::from).toList();
    }

    @GetMapping("/{id}/proposals")
    public List<ProposalResponse> proposals(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        long tenantId = JwtClaims.tenantId(jwt);
        return proposalRepository.findAllByOrderDistribution_Order_IdAndOrderDistribution_Order_Tenant_Id(id, tenantId)
                .stream().map(ProposalResponse::from).toList();
    }

    @GetMapping("/{orderId}/selection")
    public OrderSelectionResponse selection(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
        long tenantId = JwtClaims.tenantId(jwt);
        OrderSelection selection = orderSelectionRepository.findByOrder_IdAndOrder_Tenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "selection not found"));
        return OrderSelectionResponse.from(selection);
    }

    @PostMapping("/{orderId}/select")
    public OrderSelectionResponse select(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId, @Valid @RequestBody SelectProposalRequest request) {
        long tenantId = JwtClaims.tenantId(jwt);
        long userId = JwtClaims.userId(jwt);
        OrderSelection selection = orderFlowService.selectProposal(tenantId, userId, orderId, request.proposalId());
        return OrderSelectionResponse.from(selection);
    }

    @GetMapping("/{orderId}/delivery")
    public DeliveryResponse delivery(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
        long tenantId = JwtClaims.tenantId(jwt);
        OrderSelection selection = orderSelectionRepository.findByOrder_IdAndOrder_Tenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "selection not found"));

        Delivery delivery = deliveryRepository.findByOrderSelection_IdAndOrderSelection_Order_Tenant_Id(selection.getId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "delivery not found"));
        return DeliveryResponse.from(delivery);
    }

    @PostMapping("/{orderId}/rate")
    public RatingResponse rate(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId, @Valid @RequestBody CreateRatingRequest request) {
        long tenantId = JwtClaims.tenantId(jwt);
        long userId = JwtClaims.userId(jwt);
        OrderSelection selection = orderSelectionRepository.findByOrder_IdAndOrder_Tenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "selection not found"));

        Rating rating = orderFlowService.createRating(tenantId, userId, selection.getId(), request.score(), request.comment());
        return RatingResponse.from(rating);
    }

    @GetMapping("/{orderId}/proposals/{proposalId}/items")
    public List<ProposalItemResponse> proposalItems(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId, @PathVariable Long proposalId) {
        long tenantId = JwtClaims.tenantId(jwt);
        orderRepository.findByIdAndTenant_Id(orderId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "order not found"));
        proposalRepository.findByIdAndOrderDistribution_Order_Tenant_Id(proposalId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "proposal not found"));
        return proposalItemRepository.findAllByProposal_Id(proposalId).stream().map(ProposalItemResponse::from).toList();
    }

    public record CreateOrderRequest(
            @NotBlank String title,
            String description,
            Instant scheduledAt,
            @NotNull List<CreateOrderItemRequest> items
    ) {}

    public record CreateOrderItemRequest(
            @NotBlank String name,
            String unit,
            @NotNull BigDecimal quantity,
            String notes
    ) {}

    public record SelectProposalRequest(@NotNull Long proposalId) {}

    public record CreateRatingRequest(@NotNull Integer score, String comment) {}

    public record OrderSummaryResponse(Long id, String title, Order.Status status, Instant scheduledAt, Instant createdAt) {
        static OrderSummaryResponse from(Order order) {
            return new OrderSummaryResponse(order.getId(), order.getTitle(), order.getStatus(), order.getScheduledAt(), order.getCreatedAt());
        }
    }

    public record OrderDetailsResponse(
            Long id,
            String title,
            String description,
            Order.Status status,
            Instant scheduledAt,
            Instant createdAt,
            List<OrderItemResponse> items
    ) {
        static OrderDetailsResponse from(Order order, List<OrderItemResponse> items) {
            return new OrderDetailsResponse(
                    order.getId(),
                    order.getTitle(),
                    order.getDescription(),
                    order.getStatus(),
                    order.getScheduledAt(),
                    order.getCreatedAt(),
                    items
            );
        }
    }

    public record OrderItemResponse(Long id, String name, String unit, BigDecimal quantity, String notes) {
        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getId(), item.getName(), item.getUnit(), item.getQuantity(), item.getNotes());
        }
    }

    public record OrderDistributionResponse(Long id, Long orderId, Long supplierId, OrderDistribution.Status status, Instant distributedAt) {
        static OrderDistributionResponse from(OrderDistribution distribution) {
            return new OrderDistributionResponse(
                    distribution.getId(),
                    distribution.getOrder().getId(),
                    distribution.getSupplier().getId(),
                    distribution.getStatus(),
                    distribution.getDistributedAt()
            );
        }
    }

    public record ProposalResponse(Long id, Long distributionId, Proposal.Status status, BigDecimal totalPrice, Integer deliveryEtaHours, String message) {
        static ProposalResponse from(Proposal proposal) {
            return new ProposalResponse(
                    proposal.getId(),
                    proposal.getOrderDistribution().getId(),
                    proposal.getStatus(),
                    proposal.getTotalPrice(),
                    proposal.getDeliveryEtaHours(),
                    proposal.getMessage()
            );
        }
    }

    public record ProposalItemResponse(Long id, Long orderItemId, BigDecimal unitPrice, BigDecimal totalPrice, ProposalItem.Availability availability) {
        static ProposalItemResponse from(ProposalItem item) {
            return new ProposalItemResponse(item.getId(), item.getOrderItem().getId(), item.getUnitPrice(), item.getTotalPrice(), item.getAvailability());
        }
    }

    public record OrderSelectionResponse(Long id, Long orderId, Long proposalId, Long selectedBy, OrderSelection.Status status, Instant selectedAt) {
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

    public record DeliveryResponse(Long id, Long selectionId, Delivery.Status status, String trackingCode, Instant scheduledAt, Instant dispatchedAt, Instant deliveredAt, String proofUrl) {
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

    public record RatingResponse(Long id, Long selectionId, Long supplierId, int score, String comment, String response) {
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
