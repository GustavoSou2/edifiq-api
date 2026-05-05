INSERT INTO tenants (plan_id, slug, name, status, settings, trial_ends_at)
SELECT
    p.id,
    'edifiq-demo',
    'Edifiq Demo',
    'active',
    JSON_OBJECT('companyName', 'Edifiq Demo', 'currency', 'BRL'),
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 30 DAY)
FROM plans p
WHERE p.name = 'starter'
  AND NOT EXISTS (
      SELECT 1
      FROM tenants t
      WHERE t.slug = 'edifiq-demo'
  );

INSERT INTO users (tenant_id, email, password_hash, full_name, is_active, email_verified, last_login_at)
SELECT
    t.id,
    'admin@edifiq.local',
    '$2a$10$yXCqfbo6PhsiXlpqHrlujOkxqL6ugsZldCBPwdR6SFxRoECQqJlFi',
    'Administrador Demo',
    TRUE,
    TRUE,
    NULL
FROM tenants t
WHERE t.slug = 'edifiq-demo'
  AND NOT EXISTS (
      SELECT 1
      FROM users u
      WHERE u.tenant_id = t.id
        AND u.email = 'admin@edifiq.local'
  );

INSERT INTO users (tenant_id, email, password_hash, full_name, is_active, email_verified, last_login_at)
SELECT
    t.id,
    'buyer@edifiq.local',
    '$2a$10$yXCqfbo6PhsiXlpqHrlujOkxqL6ugsZldCBPwdR6SFxRoECQqJlFi',
    'Comprador Demo',
    TRUE,
    TRUE,
    NULL
FROM tenants t
WHERE t.slug = 'edifiq-demo'
  AND NOT EXISTS (
      SELECT 1
      FROM users u
      WHERE u.tenant_id = t.id
        AND u.email = 'buyer@edifiq.local'
  );
