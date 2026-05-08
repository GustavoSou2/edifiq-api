-- ============================================================
-- V7: Adiciona campo title em orders e remove unique constraint
--     de reference_code (permite múltiplos pedidos sem código)
-- ============================================================

-- Adiciona coluna title (título do pedido, obrigatório)
ALTER TABLE orders
    ADD COLUMN title VARCHAR(200) NULL COMMENT 'Título do pedido' AFTER reference_code;

-- Remove a unique constraint de reference_code
-- (múltiplos pedidos podem não ter código de referência)
ALTER TABLE orders
    DROP INDEX uq_orders_reference_code;

-- Recria como índice simples (não-único) para manter performance de busca
ALTER TABLE orders
    ADD INDEX idx_orders_reference_code (reference_code);
