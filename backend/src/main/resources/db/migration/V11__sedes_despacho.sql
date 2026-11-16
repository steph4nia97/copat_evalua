CREATE TABLE sede (
  id SMALLINT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL,
  CONSTRAINT uq_sede_nombre UNIQUE (nombre)
);

CREATE TABLE solicitud_sede (
  solicitud_id BIGINT NOT NULL,
  sede_id SMALLINT NOT NULL,
  CONSTRAINT pk_solicitud_sede PRIMARY KEY (solicitud_id, sede_id),
  CONSTRAINT fk_solicitud_sede_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT fk_solicitud_sede_sede FOREIGN KEY (sede_id) REFERENCES sede (id)
);

CREATE INDEX idx_solicitud_sede_sede ON solicitud_sede (sede_id);

INSERT INTO sede (id, nombre) VALUES
  (1, 'Antofagasta'),
  (2, 'Mejillones'),
  (3, 'Calama'),
  (4, 'María Elena'),
  (5, 'Tocopilla'),
  (6, 'Taltal');
