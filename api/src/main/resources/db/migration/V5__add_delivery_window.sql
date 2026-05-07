-- ============================================================
-- V5: Janela de entrega agendada
-- Comprador informa quando quer receber (start obrigatório,
-- end opcional para range). Fornecedor confirma a data/hora
-- exata que consegue entregar na proposta.
-- ============================================================

-- Janela desejada pelo comprador no pedido
ALTER TABLE orders
    ADD COLUMN delivery_window_start DATETIME NULL COMMENT 'Data/hora mínima desejada para receber' AFTER delivery_lng,
    ADD COLUMN delivery_window_end   DATETIME NULL COMMENT 'Data/hora máxima desejada para receber (opcional, define range)' AFTER delivery_window_start;

-- Data/hora concreta que o fornecedor propõe entregar
ALTER TABLE proposals
    ADD COLUMN proposed_delivery_at DATETIME NULL COMMENT 'Data/hora que o fornecedor confirma conseguir entregar' AFTER delivery_min;
