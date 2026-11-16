ALTER TABLE departamento DROP CONSTRAINT chk_departamento_id;
ALTER TABLE departamento ADD CONSTRAINT chk_departamento_id CHECK (id IN (1, 2, 3, 4));

INSERT INTO departamento (id, nombre) VALUES (4, 'Administración');

ALTER TABLE usuario ADD COLUMN clave_provisoria BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE solicitud ADD COLUMN veces_rechazada INTEGER NOT NULL DEFAULT 0;
ALTER TABLE solicitud ADD CONSTRAINT chk_solicitud_rechazos CHECK (veces_rechazada >= 0);

ALTER TABLE solicitud_evaluacion ADD COLUMN recibida_en TIMESTAMP;
UPDATE solicitud_evaluacion SET recibida_en = CURRENT_TIMESTAMP WHERE recibida_en IS NULL;
ALTER TABLE solicitud_evaluacion ALTER COLUMN recibida_en SET NOT NULL;

CREATE TABLE rechazo_solicitud (
  solicitud_id BIGINT PRIMARY KEY,
  descripcion VARCHAR(500) NOT NULL,
  rechazado_por_id BIGINT NOT NULL,
  destinatario_id BIGINT NOT NULL,
  rechazada_en TIMESTAMP NOT NULL,
  CONSTRAINT fk_rechazo_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT fk_rechazo_autor FOREIGN KEY (rechazado_por_id) REFERENCES usuario (id),
  CONSTRAINT fk_rechazo_destinatario FOREIGN KEY (destinatario_id) REFERENCES usuario (id),
  CONSTRAINT chk_rechazo_descripcion CHECK (char_length(descripcion) BETWEEN 1 AND 500)
);

CREATE TABLE registro_actividad (
  id BIGSERIAL PRIMARY KEY,
  fecha DATE NOT NULL,
  hora TIME NOT NULL,
  usuario_id BIGINT NOT NULL,
  departamento_id SMALLINT NOT NULL,
  accion VARCHAR(80) NOT NULL,
  detalle VARCHAR(300) NOT NULL,
  solicitud_id BIGINT,
  CONSTRAINT fk_registro_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT fk_registro_departamento FOREIGN KEY (departamento_id) REFERENCES departamento (id),
  CONSTRAINT fk_registro_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT chk_registro_accion CHECK (char_length(accion) BETWEEN 1 AND 80),
  CONSTRAINT chk_registro_detalle CHECK (char_length(detalle) BETWEEN 1 AND 300)
);

CREATE INDEX idx_registro_actividad ON registro_actividad (id DESC);

INSERT INTO registro_actividad (fecha, hora, usuario_id, departamento_id, accion, detalle, solicitud_id)
SELECT l.fecha, l.hora, l.usuario_id, u.departamento_id, l.accion, s.nombre_empresa, l.solicitud_id
FROM solicitud_log l
JOIN usuario u ON u.id = l.usuario_id
JOIN solicitud s ON s.id = l.solicitud_id;
