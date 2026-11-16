ALTER TABLE departamento DROP CONSTRAINT chk_departamento_id;
ALTER TABLE departamento ADD CONSTRAINT chk_departamento_id CHECK (id IN (1, 2, 3));

INSERT INTO departamento (id, nombre) VALUES
  (2, 'Departamento 2'),
  (3, 'Departamento 3');

CREATE TABLE solicitud_pdf (
  solicitud_id BIGINT PRIMARY KEY,
  nombre_archivo VARCHAR(200) NOT NULL,
  contenido BYTEA NOT NULL,
  CONSTRAINT fk_pdf_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT chk_pdf_nombre CHECK (char_length(nombre_archivo) BETWEEN 1 AND 200),
  CONSTRAINT chk_pdf_peso CHECK (octet_length(contenido) BETWEEN 1 AND 8000000)
);

CREATE TABLE solicitud_evaluacion (
  solicitud_id BIGINT PRIMARY KEY,
  atencion SMALLINT NOT NULL,
  claridad SMALLINT NOT NULL,
  plazos SMALLINT NOT NULL,
  resultado SMALLINT NOT NULL,
  general SMALLINT NOT NULL,
  promedio NUMERIC(3, 2) NOT NULL,
  desempeno VARCHAR(40) NOT NULL,
  CONSTRAINT fk_evaluacion_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT chk_evaluacion_notas CHECK (
    atencion BETWEEN 1 AND 5
    AND claridad BETWEEN 1 AND 5
    AND plazos BETWEEN 1 AND 5
    AND resultado BETWEEN 1 AND 5
    AND general BETWEEN 1 AND 5
  ),
  CONSTRAINT chk_evaluacion_promedio CHECK (promedio >= 1 AND promedio <= 5),
  CONSTRAINT chk_evaluacion_desempeno CHECK (char_length(desempeno) BETWEEN 1 AND 40)
);

CREATE TABLE notificacion (
  id BIGSERIAL PRIMARY KEY,
  solicitud_id BIGINT NOT NULL,
  departamento_id SMALLINT NOT NULL,
  mensaje VARCHAR(200) NOT NULL,
  creada_en TIMESTAMP NOT NULL,
  CONSTRAINT fk_notificacion_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT fk_notificacion_departamento FOREIGN KEY (departamento_id) REFERENCES departamento (id),
  CONSTRAINT uq_notificacion_solicitud_departamento UNIQUE (solicitud_id, departamento_id),
  CONSTRAINT chk_notificacion_mensaje CHECK (char_length(mensaje) BETWEEN 1 AND 200)
);

CREATE INDEX idx_notificacion_departamento ON notificacion (departamento_id, id DESC);

CREATE TABLE revision_departamento3 (
  solicitud_id BIGINT PRIMARY KEY,
  descripcion VARCHAR(500) NOT NULL,
  usuario_id BIGINT NOT NULL,
  actualizada_en TIMESTAMP NOT NULL,
  CONSTRAINT fk_revision_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT fk_revision_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT chk_revision_descripcion CHECK (char_length(descripcion) BETWEEN 1 AND 500)
);

CREATE TABLE evidencia_revision (
  solicitud_id BIGINT PRIMARY KEY,
  nombre_archivo VARCHAR(200) NOT NULL,
  media_type VARCHAR(100) NOT NULL,
  contenido BYTEA NOT NULL,
  CONSTRAINT fk_evidencia_revision FOREIGN KEY (solicitud_id) REFERENCES revision_departamento3 (solicitud_id),
  CONSTRAINT chk_evidencia_nombre CHECK (char_length(nombre_archivo) BETWEEN 1 AND 200),
  CONSTRAINT chk_evidencia_media CHECK (char_length(media_type) BETWEEN 1 AND 100),
  CONSTRAINT chk_evidencia_peso CHECK (octet_length(contenido) BETWEEN 1 AND 8000000)
);
