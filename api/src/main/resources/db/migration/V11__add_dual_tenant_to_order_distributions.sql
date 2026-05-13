ALTER TABLE order_distributions
    ADD COLUMN buyer_tenant_id CHAR(36) NULL AFTER supplier_id,
    ADD COLUMN supplier_tenant_id CHAR(36) NULL AFTER buyer_tenant_id;

UPDATE order_distributions od
JOIN orders o ON o.id = od.order_id
JOIN suppliers s ON s.id = od.supplier_id
SET od.buyer_tenant_id = o.tenant_id,
    od.supplier_tenant_id = s.tenant_id
WHERE od.buyer_tenant_id IS NULL
   OR od.supplier_tenant_id IS NULL;

ALTER TABLE order_distributions
    MODIFY COLUMN buyer_tenant_id CHAR(36) NOT NULL,
    MODIFY COLUMN supplier_tenant_id CHAR(36) NOT NULL,
    ADD KEY idx_order_dist_buyer_tenant (buyer_tenant_id),
    ADD KEY idx_order_dist_supplier_tenant (supplier_tenant_id),
    ADD CONSTRAINT fk_order_dist_buyer_tenant FOREIGN KEY (buyer_tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_order_dist_supplier_tenant FOREIGN KEY (supplier_tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
