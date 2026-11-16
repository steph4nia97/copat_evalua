ALTER TABLE reclamo
  ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'Recibido',
  ADD COLUMN area_responsable VARCHAR(80),
  ADD COLUMN responsable VARCHAR(80),
  ADD COLUMN fecha_gestion DATE,
  ADD COLUMN observaciones VARCHAR(500),
  ADD COLUMN fecha_cierre DATE,
  ADD COLUMN hora_cierre TIME,
  ADD COLUMN solucion VARCHAR(1000);

ALTER TABLE reclamo
  ADD CONSTRAINT chk_reclamo_estado CHECK (estado IN ('Recibido', 'En gestión', 'Cerrado'));
