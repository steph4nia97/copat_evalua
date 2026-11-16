ALTER TABLE reclamo
  ADD COLUMN sede_id SMALLINT;

ALTER TABLE reclamo
  ADD CONSTRAINT fk_reclamo_sede FOREIGN KEY (sede_id) REFERENCES sede (id);

CREATE INDEX idx_reclamo_sede ON reclamo (sede_id);
