-- ============================================================
-- V6: Adiciona delivery_eta_hours em proposals
-- Campo que existia na entidade Java mas estava faltando
-- no schema do banco. Adicionado separadamente pois a V5
-- falhou ao tentar referenciar esta coluna com AFTER.
-- ============================================================

ALTER TABLE proposals
    ADD COLUMN delivery_eta_hours INT NULL COMMENT 'ETA em horas informado pelo fornecedor' AFTER delivery_min;
