-- =============================================================
-- Schema: Plataforma de Leilão Reverso de Materiais de Construção
-- Banco: MySQL 8.0+
-- Padrão: Multi-tenant SaaS com RBAC
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- ========================
-- PLANOS
-- ========================
CREATE TABLE plans (
                       id                   CHAR(36)       NOT NULL DEFAULT (UUID()),
                       name                 VARCHAR(50)    NOT NULL,
                       max_users            INT            NOT NULL DEFAULT 5,
                       max_suppliers        INT            NOT NULL DEFAULT 50,
                       max_orders_per_month INT            NOT NULL DEFAULT 100,
                       has_analytics        TINYINT(1)     NOT NULL DEFAULT 0,
                       has_api_access       TINYINT(1)     NOT NULL DEFAULT 0,
                       price_monthly        DECIMAL(10, 2) NOT NULL DEFAULT 0,
                       created_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       PRIMARY KEY (id),
                       UNIQUE KEY uq_plans_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Planos de assinatura com limites e funcionalidades';

INSERT INTO plans (id, name, max_users, max_suppliers, max_orders_per_month, has_analytics, has_api_access, price_monthly)
VALUES
    (UUID(), 'free',        3,    20,    30, 0, 0,    0.00),
    (UUID(), 'starter',    10,   100,   200, 0, 0,  199.90),
    (UUID(), 'pro',        30,   500,  1000, 1, 0,  599.90),
    (UUID(), 'enterprise', 999, 9999, 99999, 1, 1, 1999.90);

-- ========================
-- TENANTS
-- ========================
CREATE TABLE tenants (
                         id            CHAR(36)     NOT NULL DEFAULT (UUID()),
                         plan_id       CHAR(36)     NOT NULL,
                         slug          VARCHAR(60)  NOT NULL,
                         name          VARCHAR(150) NOT NULL,
                         cnpj          VARCHAR(18)  NULL,
                         status        ENUM('active','suspended','cancelled','trial') NOT NULL DEFAULT 'trial',
                         settings      JSON         NOT NULL,
                         trial_ends_at DATETIME     NULL,
                         created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         PRIMARY KEY (id),
                         UNIQUE KEY uq_tenants_slug (slug),
                         UNIQUE KEY uq_tenants_cnpj (cnpj),
                         KEY idx_tenants_status (status),
                         CONSTRAINT fk_tenants_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Empresas clientes do SaaS (cada uma é um tenant isolado)';

-- ========================
-- USUÁRIOS
-- ========================
CREATE TABLE users (
                       id             CHAR(36)     NOT NULL DEFAULT (UUID()),
                       tenant_id      CHAR(36)     NOT NULL,
                       email          VARCHAR(255) NOT NULL,
                       password_hash  VARCHAR(255) NOT NULL,
                       full_name      VARCHAR(150) NOT NULL,
                       phone          VARCHAR(20)  NULL,
                       avatar_url     TEXT         NULL,
                       is_active      TINYINT(1)   NOT NULL DEFAULT 1,
                       email_verified TINYINT(1)   NOT NULL DEFAULT 0,
                       last_login_at  DATETIME     NULL,
                       created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       PRIMARY KEY (id),
                       UNIQUE KEY uq_users_tenant_email (tenant_id, email),
                       KEY idx_users_tenant_id (tenant_id),
                       KEY idx_users_email (email),
                       CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Usuários de cada tenant com acesso à plataforma';

-- ========================
-- CONTROLE DE ACESSO (RBAC)
-- ========================
CREATE TABLE roles (
                       id          CHAR(36)     NOT NULL DEFAULT (UUID()),
                       tenant_id   CHAR(36)     NOT NULL,
                       name        VARCHAR(80)  NOT NULL,
                       description TEXT         NULL,
                       permissions JSON         NOT NULL,
                       is_system   TINYINT(1)   NOT NULL DEFAULT 0,
                       created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       PRIMARY KEY (id),
                       UNIQUE KEY uq_roles_tenant_name (tenant_id, name),
                       KEY idx_roles_tenant_id (tenant_id),
                       CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Perfis de permissão por tenant (RBAC)';

CREATE TABLE user_roles (
                            user_id    CHAR(36)  NOT NULL,
                            role_id    CHAR(36)  NOT NULL,
                            granted_by CHAR(36)  NULL,
                            granted_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_user    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_roles_role    FOREIGN KEY (role_id)    REFERENCES roles(id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_roles_granter FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Vínculo entre usuários e perfis de acesso';

-- ========================
-- CATEGORIAS DE MATERIAIS
-- ========================
CREATE TABLE categories (
                            id         CHAR(36)     NOT NULL DEFAULT (UUID()),
                            parent_id  CHAR(36)     NULL,
                            name       VARCHAR(100) NOT NULL,
                            slug       VARCHAR(100) NOT NULL,
                            created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            PRIMARY KEY (id),
                            UNIQUE KEY uq_categories_slug (slug),
                            CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Categorias globais de materiais de construção';

INSERT INTO categories (id, name, slug) VALUES
                                            (UUID(), 'Cimento e Argamassa',    'cimento-argamassa'),
                                            (UUID(), 'Areia e Brita',          'areia-brita'),
                                            (UUID(), 'Cerâmica e Pisos',       'ceramica-pisos'),
                                            (UUID(), 'Tintas e Revestimentos', 'tintas-revestimentos'),
                                            (UUID(), 'Ferragens e Fixadores',  'ferragens-fixadores'),
                                            (UUID(), 'Hidráulica',             'hidraulica'),
                                            (UUID(), 'Elétrica',               'eletrica'),
                                            (UUID(), 'Madeira e Estruturas',   'madeira-estruturas'),
                                            (UUID(), 'Ferramentas',            'ferramentas'),
                                            (UUID(), 'EPI e Segurança',        'epi-seguranca');

-- ========================
-- FORNECEDORES
-- ========================
CREATE TABLE suppliers (
                           id               CHAR(36)     NOT NULL DEFAULT (UUID()),
                           tenant_id        CHAR(36)     NOT NULL,
                           company_name     VARCHAR(200) NOT NULL,
                           cnpj             VARCHAR(18)  NOT NULL,
                           email            VARCHAR(255) NOT NULL,
                           phone            VARCHAR(20)  NULL,
                           status           ENUM('active','inactive','blocked') NOT NULL DEFAULT 'active',
    -- Localização
                           address          TEXT         NULL,
                           city             VARCHAR(100) NULL,
                           state            CHAR(2)      NULL,
                           zip_code         VARCHAR(9)   NULL,
                           lat              DECIMAL(9,6) NULL,
                           lng              DECIMAL(9,6) NULL,
    -- Reputação
                           reputation_score DECIMAL(3,2) NOT NULL DEFAULT 5.00,
                           total_ratings    INT          NOT NULL DEFAULT 0,
                           total_deliveries INT          NOT NULL DEFAULT 0,
    -- Configurações
                           max_delivery_km  INT          NOT NULL DEFAULT 50,
                           response_sla_min INT          NOT NULL DEFAULT 60,
                           created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                           PRIMARY KEY (id),
                           UNIQUE KEY uq_suppliers_tenant_cnpj (tenant_id, cnpj),
                           KEY idx_suppliers_tenant_id (tenant_id),
                           KEY idx_suppliers_status    (status),
                           KEY idx_suppliers_location  (lat, lng),
                           CONSTRAINT fk_suppliers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
                           CONSTRAINT chk_suppliers_score CHECK (reputation_score BETWEEN 0 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Fornecedores cadastrados por tenant para receber pedidos';

CREATE TABLE supplier_categories (
                                     supplier_id CHAR(36) NOT NULL,
                                     category_id CHAR(36) NOT NULL,

                                     PRIMARY KEY (supplier_id, category_id),
                                     CONSTRAINT fk_sup_cat_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_sup_cat_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Categorias de materiais atendidas por cada fornecedor';

-- ========================
-- PEDIDOS
-- ========================
CREATE TABLE orders (
                        id                   CHAR(36)     NOT NULL DEFAULT (UUID()),
                        tenant_id            CHAR(36)     NOT NULL,
                        created_by           CHAR(36)     NOT NULL,
                        status               ENUM('draft','open','in_auction','selected','confirmed','cancelled','expired') NOT NULL DEFAULT 'draft',
                        is_urgent            TINYINT(1)   NOT NULL DEFAULT 0,
    -- Entrega
                        delivery_address     TEXT         NOT NULL,
                        delivery_city        VARCHAR(100) NULL,
                        delivery_state       CHAR(2)      NULL,
                        delivery_lat         DECIMAL(9,6) NULL,
                        delivery_lng         DECIMAL(9,6) NULL,
    -- Configurações do leilão
                        max_suppliers        INT          NOT NULL DEFAULT 10,
                        auction_duration_min INT          NOT NULL DEFAULT 60,
    -- Datas
                        expires_at           DATETIME     NULL,
                        published_at         DATETIME     NULL,
                        created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- Metadados
                        notes                TEXT         NULL,
                        reference_code       VARCHAR(50)  NULL,
                        metadata             JSON         NOT NULL,

                        PRIMARY KEY (id),
                        UNIQUE KEY uq_orders_reference_code (reference_code),
                        KEY idx_orders_tenant_id  (tenant_id),
                        KEY idx_orders_status     (status),
                        KEY idx_orders_created_by (created_by),
                        KEY idx_orders_expires_at (expires_at),
                        CONSTRAINT fk_orders_tenant     FOREIGN KEY (tenant_id)  REFERENCES tenants(id) ON DELETE CASCADE,
                        CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Pedidos de materiais criados pelos compradores';

CREATE TABLE order_items (
                             id          CHAR(36)        NOT NULL DEFAULT (UUID()),
                             order_id    CHAR(36)        NOT NULL,
                             category_id CHAR(36)        NULL,
                             description VARCHAR(300)    NOT NULL,
                             quantity    DECIMAL(12, 3)  NOT NULL,
                             unit        VARCHAR(20)     NOT NULL,
                             notes       TEXT            NULL,
                             sort_order  INT             NOT NULL DEFAULT 0,

                             PRIMARY KEY (id),
                             KEY idx_order_items_order_id (order_id),
                             CONSTRAINT fk_order_items_order    FOREIGN KEY (order_id)    REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_order_items_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Itens individuais de cada pedido';

-- ========================
-- DISTRIBUIÇÃO DE PEDIDOS
-- ========================
CREATE TABLE order_distributions (
                                     id          CHAR(36)    NOT NULL DEFAULT (UUID()),
                                     order_id    CHAR(36)    NOT NULL,
                                     supplier_id CHAR(36)    NOT NULL,
                                     notified_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     channel     VARCHAR(30) NOT NULL DEFAULT 'email',
                                     opened_at   DATETIME    NULL,

                                     PRIMARY KEY (id),
                                     UNIQUE KEY uq_order_dist (order_id, supplier_id),
                                     KEY idx_order_dist_order_id    (order_id),
                                     KEY idx_order_dist_supplier_id (supplier_id),
                                     CONSTRAINT fk_order_dist_order    FOREIGN KEY (order_id)    REFERENCES orders(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_order_dist_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Registro de fornecedores notificados para cada pedido';

-- ========================
-- PROPOSTAS (LEILÃO REVERSO)
-- ========================
CREATE TABLE proposals (
                           id           CHAR(36)       NOT NULL DEFAULT (UUID()),
                           order_id     CHAR(36)       NOT NULL,
                           supplier_id  CHAR(36)       NOT NULL,
                           status       ENUM('pending','submitted','accepted','rejected','expired','withdrawn') NOT NULL DEFAULT 'pending',
                           total_price  DECIMAL(12, 2) NOT NULL,
                           delivery_min INT            NOT NULL,
                           notes        TEXT           NULL,
                           submitted_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           expires_at   DATETIME       NULL,
                           updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                           PRIMARY KEY (id),
                           UNIQUE KEY uq_proposals_order_supplier (order_id, supplier_id),
                           KEY idx_proposals_order_id    (order_id),
                           KEY idx_proposals_supplier_id (supplier_id),
                           KEY idx_proposals_status      (status),
                           CONSTRAINT fk_proposals_order    FOREIGN KEY (order_id)    REFERENCES orders(id) ON DELETE CASCADE,
                           CONSTRAINT fk_proposals_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Propostas dos fornecedores no leilão reverso';

CREATE TABLE proposal_items (
                                id            CHAR(36)       NOT NULL DEFAULT (UUID()),
                                proposal_id   CHAR(36)       NOT NULL,
                                order_item_id CHAR(36)       NOT NULL,
                                unit_price    DECIMAL(12, 2) NOT NULL,
                                quantity      DECIMAL(12, 3) NOT NULL,
                                available     TINYINT(1)     NOT NULL DEFAULT 1,
                                notes         TEXT           NULL,

                                PRIMARY KEY (id),
                                UNIQUE KEY uq_proposal_items (proposal_id, order_item_id),
                                KEY idx_proposal_items_proposal_id (proposal_id),
                                CONSTRAINT fk_prop_items_proposal   FOREIGN KEY (proposal_id)   REFERENCES proposals(id) ON DELETE CASCADE,
                                CONSTRAINT fk_prop_items_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cotação item a item de cada proposta';

-- ========================
-- SELEÇÃO (DECISÃO DO COMPRADOR)
-- ========================
CREATE TABLE order_selections (
                                  id          CHAR(36)  NOT NULL DEFAULT (UUID()),
                                  order_id    CHAR(36)  NOT NULL,
                                  proposal_id CHAR(36)  NOT NULL,
                                  selected_by CHAR(36)  NOT NULL,
                                  selected_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  reason      TEXT      NULL,

                                  PRIMARY KEY (id),
                                  UNIQUE KEY uq_order_selections_order (order_id),
                                  CONSTRAINT fk_sel_order    FOREIGN KEY (order_id)    REFERENCES orders(id),
                                  CONSTRAINT fk_sel_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id),
                                  CONSTRAINT fk_sel_user     FOREIGN KEY (selected_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Seleção final do comprador (encerra o leilão)';

-- ========================
-- ENTREGAS
-- ========================
CREATE TABLE deliveries (
                            id                 CHAR(36)   NOT NULL DEFAULT (UUID()),
                            order_selection_id CHAR(36)   NOT NULL,
                            status             ENUM('scheduled','in_transit','delivered','failed','returned') NOT NULL DEFAULT 'scheduled',
                            tracking_code      VARCHAR(100) NULL,
                            scheduled_at       DATETIME   NULL,
                            dispatched_at      DATETIME   NULL,
                            delivered_at       DATETIME   NULL,
                            delivery_notes     TEXT       NULL,
                            proof_url          TEXT       NULL,
                            created_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            PRIMARY KEY (id),
                            CONSTRAINT fk_deliveries_selection FOREIGN KEY (order_selection_id) REFERENCES order_selections(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Acompanhamento da entrega pós-seleção';

-- ========================
-- AVALIAÇÕES (REPUTAÇÃO)
-- ========================
CREATE TABLE ratings (
                         id                 CHAR(36)  NOT NULL DEFAULT (UUID()),
                         order_selection_id CHAR(36)  NOT NULL,
                         rated_by           CHAR(36)  NOT NULL,
                         supplier_id        CHAR(36)  NOT NULL,
                         score              TINYINT   NOT NULL,
                         comment            TEXT      NULL,
                         response           TEXT      NULL,
                         created_at         DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         PRIMARY KEY (id),
                         UNIQUE KEY uq_ratings_selection_user (order_selection_id, rated_by),
                         KEY idx_ratings_supplier_id (supplier_id),
                         CONSTRAINT fk_ratings_selection FOREIGN KEY (order_selection_id) REFERENCES order_selections(id),
                         CONSTRAINT fk_ratings_user      FOREIGN KEY (rated_by)           REFERENCES users(id),
                         CONSTRAINT fk_ratings_supplier  FOREIGN KEY (supplier_id)        REFERENCES suppliers(id),
                         CONSTRAINT chk_ratings_score    CHECK (score BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Avaliações dos compradores sobre os fornecedores';

-- ========================
-- WEBHOOKS
-- ========================
CREATE TABLE webhooks (
                          id        CHAR(36)     NOT NULL DEFAULT (UUID()),
                          tenant_id CHAR(36)     NOT NULL,
                          url       TEXT         NOT NULL,
                          events    JSON         NOT NULL,
                          secret    VARCHAR(100) NOT NULL,
                          is_active TINYINT(1)   NOT NULL DEFAULT 1,
                          created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          PRIMARY KEY (id),
                          KEY idx_webhooks_tenant_id (tenant_id),
                          CONSTRAINT fk_webhooks_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Endpoints de integração para notificações de eventos';

CREATE TABLE webhook_deliveries (
                                    id          CHAR(36)    NOT NULL DEFAULT (UUID()),
                                    webhook_id  CHAR(36)    NOT NULL,
                                    event       VARCHAR(80) NOT NULL,
                                    payload     JSON        NOT NULL,
                                    status_code SMALLINT    NULL,
                                    response    TEXT        NULL,
                                    delivered_at DATETIME   NULL,
                                    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    PRIMARY KEY (id),
                                    KEY idx_webhook_del_webhook_id (webhook_id),
                                    CONSTRAINT fk_webhook_del_webhook FOREIGN KEY (webhook_id) REFERENCES webhooks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Histórico de disparos de webhooks';

-- ========================
-- AUDIT LOG
-- ========================
CREATE TABLE audit_logs (
                            id         CHAR(36)    NOT NULL DEFAULT (UUID()),
                            tenant_id  CHAR(36)    NOT NULL,
                            user_id    CHAR(36)    NULL,
                            action     VARCHAR(80) NOT NULL,
                            entity     VARCHAR(60) NOT NULL,
                            entity_id  CHAR(36)    NULL,
                            payload    JSON        NOT NULL,
                            ip_address VARCHAR(45) NULL,
                            user_agent TEXT        NULL,
                            created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            PRIMARY KEY (id),
                            KEY idx_audit_tenant_id  (tenant_id),
                            KEY idx_audit_user_id    (user_id),
                            KEY idx_audit_entity     (entity, entity_id),
                            KEY idx_audit_created_at (created_at),
                            CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
                            CONSTRAINT fk_audit_user   FOREIGN KEY (user_id)   REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Registro imutável de todas as ações para auditoria';

-- ========================
-- TRIGGERS
-- ========================

-- Recalcula reputation_score após nova avaliação
DELIMITER $$

CREATE TRIGGER trg_update_supplier_reputation_insert
    AFTER INSERT ON ratings
    FOR EACH ROW
BEGIN
    UPDATE suppliers
    SET
        reputation_score = (
            SELECT ROUND(AVG(score), 2)
            FROM ratings
            WHERE supplier_id = NEW.supplier_id
        ),
        total_ratings = (
            SELECT COUNT(*)
            FROM ratings
            WHERE supplier_id = NEW.supplier_id
        )
    WHERE id = NEW.supplier_id;
    END$$

    CREATE TRIGGER trg_update_supplier_reputation_update
        AFTER UPDATE ON ratings
        FOR EACH ROW
    BEGIN
        UPDATE suppliers
        SET
            reputation_score = (
                SELECT ROUND(AVG(score), 2)
                FROM ratings
                WHERE supplier_id = NEW.supplier_id
            ),
            total_ratings = (
                SELECT COUNT(*)
                FROM ratings
                WHERE supplier_id = NEW.supplier_id
            )
        WHERE id = NEW.supplier_id;
        END$$

        DELIMITER ;

SET FOREIGN_KEY_CHECKS = 1;
