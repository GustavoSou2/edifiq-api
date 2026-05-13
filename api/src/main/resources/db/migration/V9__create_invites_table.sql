CREATE TABLE invites (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    tenant_id   CHAR(36)     NOT NULL,
    email       VARCHAR(255) NOT NULL,
    token       VARCHAR(255) NOT NULL,
    invited_by  CHAR(36)     NOT NULL,
    role        VARCHAR(100) NULL,
    status      ENUM('PENDING','ACCEPTED','EXPIRED','CANCELED') NOT NULL DEFAULT 'PENDING',
    expires_at  DATETIME     NOT NULL,
    accepted_at DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_invites_token (token),
    KEY idx_invites_tenant_id (tenant_id),
    KEY idx_invites_email (email),
    KEY idx_invites_status_expires_at (status, expires_at),
    CONSTRAINT fk_invites_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_invites_invited_by FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Convites de acesso de usuarios por tenant';
