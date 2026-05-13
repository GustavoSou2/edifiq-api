ALTER TABLE invites
    ADD COLUMN role_id CHAR(36) NULL AFTER invited_by;

UPDATE invites i
JOIN roles r
    ON r.tenant_id = i.tenant_id
   AND LOWER(r.name) = LOWER(i.role)
SET i.role_id = r.id
WHERE i.role_id IS NULL
  AND i.role IS NOT NULL;

ALTER TABLE invites
    MODIFY COLUMN role_id CHAR(36) NOT NULL,
    ADD KEY idx_invites_role_id (role_id),
    ADD CONSTRAINT fk_invites_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT;

ALTER TABLE invites
    DROP COLUMN role;
