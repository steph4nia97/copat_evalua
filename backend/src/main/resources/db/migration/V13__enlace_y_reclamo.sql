CREATE TABLE enlace_encuesta (
  solicitud_id BIGINT PRIMARY KEY,
  token VARCHAR(64) NOT NULL,
  creado_en TIMESTAMP NOT NULL,
  CONSTRAINT uq_enlace_encuesta_token UNIQUE (token),
  CONSTRAINT fk_enlace_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id) ON DELETE CASCADE,
  CONSTRAINT chk_enlace_token CHECK (char_length(token) BETWEEN 20 AND 64)
);

CREATE TABLE reclamo (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  telefono VARCHAR(20) NOT NULL,
  obra VARCHAR(200) NOT NULL,
  fecha_servicio DATE NOT NULL,
  empresa VARCHAR(150) NOT NULL,
  correo VARCHAR(254) NOT NULL,
  ubicacion VARCHAR(200) NOT NULL,
  guia_despacho VARCHAR(40),
  tipo VARCHAR(40) NOT NULL,
  descripcion VARCHAR(1000) NOT NULL,
  preferencia_contacto VARCHAR(20) NOT NULL,
  horario VARCHAR(40),
  comentarios VARCHAR(500),
  recibido_en TIMESTAMP NOT NULL,
  CONSTRAINT chk_reclamo_nombre CHECK (char_length(nombre) BETWEEN 1 AND 120),
  CONSTRAINT chk_reclamo_obra CHECK (char_length(obra) BETWEEN 1 AND 200),
  CONSTRAINT chk_reclamo_empresa CHECK (char_length(empresa) BETWEEN 1 AND 150),
  CONSTRAINT chk_reclamo_ubicacion CHECK (char_length(ubicacion) BETWEEN 1 AND 200),
  CONSTRAINT chk_reclamo_descripcion CHECK (char_length(descripcion) BETWEEN 1 AND 1000),
  CONSTRAINT chk_reclamo_tipo CHECK (tipo IN (
    'Calidad del hormigón',
    'Retraso en la entrega',
    'Atención del personal',
    'Facturación / documentación',
    'Otro')),
  CONSTRAINT chk_reclamo_preferencia CHECK (preferencia_contacto IN ('Teléfono', 'Mail'))
);

CREATE INDEX idx_reclamo_recibido ON reclamo (id DESC);
