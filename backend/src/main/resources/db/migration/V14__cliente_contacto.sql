CREATE TABLE cliente (
  rut VARCHAR(12) PRIMARY KEY,
  nombre_empresa VARCHAR(150) NOT NULL,
  CONSTRAINT chk_cliente_rut CHECK (rut ~ '^[0-9]{7,8}-[0-9K]$'),
  CONSTRAINT chk_cliente_empresa CHECK (char_length(nombre_empresa) BETWEEN 1 AND 150)
);

CREATE TABLE contacto (
  id BIGSERIAL PRIMARY KEY,
  rut VARCHAR(12) NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  empresa VARCHAR(150) NOT NULL,
  correo VARCHAR(254) NOT NULL,
  telefono VARCHAR(20),
  CONSTRAINT fk_contacto_cliente FOREIGN KEY (rut) REFERENCES cliente (rut),
  CONSTRAINT uq_contacto_rut_correo UNIQUE (rut, correo),
  CONSTRAINT chk_contacto_nombre CHECK (char_length(nombre) BETWEEN 1 AND 120),
  CONSTRAINT chk_contacto_empresa CHECK (char_length(empresa) BETWEEN 1 AND 150),
  CONSTRAINT chk_contacto_correo CHECK (char_length(correo) BETWEEN 3 AND 254)
);

CREATE INDEX idx_contacto_rut ON contacto (rut);
