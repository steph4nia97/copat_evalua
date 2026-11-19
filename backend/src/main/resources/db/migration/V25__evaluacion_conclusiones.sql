ALTER TABLE solicitud ADD COLUMN conclusiones VARCHAR(2000);

ALTER TABLE solicitud ADD CONSTRAINT chk_solicitud_conclusiones
  CHECK (conclusiones IS NULL OR char_length(conclusiones) BETWEEN 1 AND 2000);

CREATE TABLE accion_evaluacion (
  id BIGSERIAL PRIMARY KEY,
  solicitud_id BIGINT NOT NULL,
  descripcion VARCHAR(400) NOT NULL,
  responsable VARCHAR(80) NOT NULL,
  plazo DATE NOT NULL,
  estado VARCHAR(20) NOT NULL,
  CONSTRAINT fk_accion_evaluacion_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id) ON DELETE CASCADE,
  CONSTRAINT chk_accion_evaluacion_estado CHECK (estado IN ('En gestión', 'Cerrado')),
  CONSTRAINT chk_accion_evaluacion_descripcion CHECK (char_length(btrim(descripcion)) BETWEEN 1 AND 400),
  CONSTRAINT chk_accion_evaluacion_responsable CHECK (char_length(btrim(responsable)) BETWEEN 1 AND 80)
);

CREATE INDEX idx_accion_evaluacion_solicitud ON accion_evaluacion (solicitud_id, id);
