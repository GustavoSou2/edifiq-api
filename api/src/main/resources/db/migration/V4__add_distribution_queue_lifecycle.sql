ALTER TABLE order_distributions
    ADD COLUMN queue_message_id VARCHAR(100) NULL AFTER opened_at,
    ADD COLUMN queued_at TIMESTAMP(6) NULL AFTER queue_message_id,
    ADD COLUMN processing_at TIMESTAMP(6) NULL AFTER queued_at,
    ADD COLUMN sent_at TIMESTAMP(6) NULL AFTER processing_at,
    ADD COLUMN failed_at TIMESTAMP(6) NULL AFTER sent_at,
    ADD COLUMN dispatch_attempts INT NOT NULL DEFAULT 0 AFTER failed_at,
    ADD COLUMN failure_reason TEXT NULL AFTER dispatch_attempts;