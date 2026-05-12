-- Adiciona coluna `id` à tabela user_roles para compatibilidade com BaseEntity (JPA UUID PK).
-- Usa SHA1 sobre valores determinísticos da própria linha para gerar IDs únicos
-- sem depender de funções não-determinísticas (UUID(), RAND()) bloqueadas em replicação.

-- 1. Adiciona a coluna id como nullable
ALTER TABLE user_roles
    ADD COLUMN id CHAR(36) NULL FIRST;

-- 2. Popula registros existentes com um UUID v4-like derivado de SHA1(user_id + role_id)
--    Formato: 8-4-4-4-12 usando os primeiros 32 hex chars do SHA1
UPDATE user_roles
SET id = LOWER(CONCAT(
    SUBSTR(SHA1(CONCAT(user_id, role_id, granted_at)), 1,  8), '-',
    SUBSTR(SHA1(CONCAT(user_id, role_id, granted_at)), 9,  4), '-',
    '4',
    SUBSTR(SHA1(CONCAT(user_id, role_id, granted_at)), 14, 3), '-',
    SUBSTR(SHA1(CONCAT(user_id, role_id, granted_at)), 17, 4), '-',
    SUBSTR(SHA1(CONCAT(user_id, role_id, granted_at)), 21, 12)
))
WHERE id IS NULL;

-- 3. Torna NOT NULL
ALTER TABLE user_roles
    MODIFY COLUMN id CHAR(36) NOT NULL;

-- 4. Substitui PK composta por PK simples, mantendo unicidade em (user_id, role_id)
ALTER TABLE user_roles
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (id),
    ADD UNIQUE KEY uq_user_roles_user_role (user_id, role_id);
