ALTER TABLE informe_gestion_reclamo
    ADD comercial_usuario_id BIGINT REFERENCES usuario (id) ON DELETE SET NULL,
    ADD comercial_cargo VARCHAR(80),
    ADD comercial_firma_virtual BOOLEAN NOT NULL DEFAULT FALSE,
    ADD gerencia_usuario_id BIGINT REFERENCES usuario (id) ON DELETE SET NULL,
    ADD gerencia_cargo VARCHAR(80),
    ADD gerencia_firma_virtual BOOLEAN NOT NULL DEFAULT FALSE;
