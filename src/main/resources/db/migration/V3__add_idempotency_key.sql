ALTER TABLE transferencia_entity
    ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE transferencia_entity
    ADD CONSTRAINT uk_transferencia_idempotency_key
        UNIQUE (idempotency_key);