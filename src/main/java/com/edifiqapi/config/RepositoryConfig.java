package com.edifiqapi.config;

import com.edifiqapi.domain.audit.AuditLog;
import com.edifiqapi.domain.catalog.Category;
import com.edifiqapi.domain.delivery.Delivery;
import com.edifiqapi.domain.delivery.Rating;
import com.edifiqapi.domain.order.Order;
import com.edifiqapi.domain.order.OrderDistribution;
import com.edifiqapi.domain.order.OrderItem;
import com.edifiqapi.domain.order.OrderSelection;
import com.edifiqapi.domain.plan.Plan;
import com.edifiqapi.domain.proposal.Proposal;
import com.edifiqapi.domain.proposal.ProposalItem;
import com.edifiqapi.domain.rbac.Role;
import com.edifiqapi.domain.rbac.User;
import com.edifiqapi.domain.rbac.UserRole;
import com.edifiqapi.domain.supplier.Supplier;
import com.edifiqapi.domain.supplier.SupplierCategory;
import com.edifiqapi.domain.tenant.Tenant;
import com.edifiqapi.domain.webhook.Webhook;
import com.edifiqapi.domain.webhook.WebhookDelivery;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class RepositoryConfig implements RepositoryRestConfigurer {
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.exposeIdsFor(
                Plan.class,
                Tenant.class,
                User.class,
                Role.class,
                UserRole.class,
                Category.class,
                Supplier.class,
                SupplierCategory.class,
                Order.class,
                OrderItem.class,
                OrderDistribution.class,
                Proposal.class,
                ProposalItem.class,
                OrderSelection.class,
                Delivery.class,
                Rating.class,
                Webhook.class,
                WebhookDelivery.class,
                AuditLog.class
        );
    }
}
