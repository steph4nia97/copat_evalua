CREATE OR REPLACE FUNCTION rut_modulo11_valido(rut text) RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  limpio text;
  cuerpo text;
  dv text;
  suma integer := 0;
  factor integer := 2;
  i integer;
  resto integer;
  esperado text;
BEGIN
  IF rut IS NULL THEN
    RETURN false;
  END IF;
  limpio := upper(replace(replace(replace(rut, '.', ''), '-', ''), ' ', ''));
  IF limpio !~ '^[0-9]{7,8}[0-9K]$' THEN
    RETURN false;
  END IF;
  cuerpo := substring(limpio FROM 1 FOR char_length(limpio) - 1);
  dv := substring(limpio FROM char_length(limpio) FOR 1);
  FOR i IN REVERSE char_length(cuerpo)..1 LOOP
    suma := suma + substring(cuerpo FROM i FOR 1)::integer * factor;
    IF factor = 7 THEN
      factor := 2;
    ELSE
      factor := factor + 1;
    END IF;
  END LOOP;
  resto := 11 - (suma % 11);
  esperado := CASE
    WHEN resto = 11 THEN '0'
    WHEN resto = 10 THEN 'K'
    ELSE resto::text
  END;
  RETURN dv = esperado;
END;
$$;

CREATE TABLE departamento (
  id SMALLINT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL,
  CONSTRAINT uq_departamento_nombre UNIQUE (nombre),
  CONSTRAINT chk_departamento_id CHECK (id = 1)
);

CREATE TABLE usuario (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  nombre VARCHAR(80) NOT NULL,
  departamento_id SMALLINT NOT NULL,
  CONSTRAINT uq_usuario_email UNIQUE (email),
  CONSTRAINT fk_usuario_departamento FOREIGN KEY (departamento_id) REFERENCES departamento (id),
  CONSTRAINT chk_usuario_email CHECK (email ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'),
  CONSTRAINT chk_usuario_password CHECK (password_hash !~ '^semilla1$' AND char_length(password_hash) >= 60)
);

CREATE INDEX idx_usuario_departamento ON usuario (departamento_id);

CREATE TABLE plantilla (
  id SMALLINT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL,
  CONSTRAINT uq_plantilla_nombre UNIQUE (nombre)
);

CREATE TABLE estado_solicitud (
  id SMALLINT PRIMARY KEY,
  nombre VARCHAR(40) NOT NULL,
  CONSTRAINT uq_estado_solicitud_nombre UNIQUE (nombre)
);

CREATE TABLE solicitud (
  id BIGSERIAL PRIMARY KEY,
  nombre_empresa VARCHAR(100) NOT NULL,
  rut VARCHAR(12) NOT NULL,
  correo VARCHAR(254) NOT NULL,
  telefono VARCHAR(11),
  periodo_inicio DATE NOT NULL,
  periodo_fin DATE NOT NULL,
  plantilla_id SMALLINT NOT NULL,
  descripcion VARCHAR(300),
  estado_id SMALLINT NOT NULL,
  departamento_id SMALLINT NOT NULL,
  creado_por_id BIGINT NOT NULL,
  CONSTRAINT fk_solicitud_plantilla FOREIGN KEY (plantilla_id) REFERENCES plantilla (id),
  CONSTRAINT fk_solicitud_estado FOREIGN KEY (estado_id) REFERENCES estado_solicitud (id),
  CONSTRAINT fk_solicitud_departamento FOREIGN KEY (departamento_id) REFERENCES departamento (id),
  CONSTRAINT fk_solicitud_usuario FOREIGN KEY (creado_por_id) REFERENCES usuario (id),
  CONSTRAINT uq_solicitud_rut_periodo UNIQUE (rut, periodo_inicio, periodo_fin),
  CONSTRAINT chk_solicitud_nombre CHECK (
    char_length(nombre_empresa) BETWEEN 1 AND 100
    AND nombre_empresa ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]+( [A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]+)*$'
  ),
  CONSTRAINT chk_solicitud_rut_forma CHECK (rut ~ '^[0-9]{7,8}-[0-9K]$'),
  CONSTRAINT chk_solicitud_rut_modulo CHECK (rut_modulo11_valido(rut)),
  CONSTRAINT chk_solicitud_correo CHECK (correo ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'),
  CONSTRAINT chk_solicitud_telefono CHECK (telefono IS NULL OR telefono ~ '^(56)?[2-9][0-9]{8}$'),
  CONSTRAINT chk_solicitud_periodo CHECK (periodo_fin >= periodo_inicio),
  CONSTRAINT chk_solicitud_descripcion CHECK (descripcion IS NULL OR char_length(descripcion) BETWEEN 1 AND 300)
);

CREATE INDEX idx_solicitud_estado ON solicitud (estado_id);
CREATE INDEX idx_solicitud_departamento ON solicitud (departamento_id);
CREATE INDEX idx_solicitud_rut ON solicitud (rut);
CREATE INDEX idx_solicitud_creado_por ON solicitud (creado_por_id);

CREATE TABLE solicitud_log (
  id BIGSERIAL PRIMARY KEY,
  solicitud_id BIGINT NOT NULL,
  fecha DATE NOT NULL,
  hora TIME NOT NULL,
  usuario_id BIGINT NOT NULL,
  accion VARCHAR(80) NOT NULL,
  CONSTRAINT fk_log_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT fk_log_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT chk_log_accion CHECK (char_length(accion) BETWEEN 1 AND 80)
);

CREATE INDEX idx_log_solicitud ON solicitud_log (solicitud_id);
CREATE INDEX idx_log_usuario ON solicitud_log (usuario_id);
CREATE INDEX idx_log_fecha ON solicitud_log (fecha, hora);

INSERT INTO departamento (id, nombre) VALUES (1, 'Departamento 1');

INSERT INTO plantilla (id, nombre) VALUES
  (1, 'Guia evaluacion'),
  (2, 'Guia evaluacion 2');

INSERT INTO estado_solicitud (id, nombre) VALUES
  (1, 'solicitud creada'),
  (2, 'solicitud enviada a cliente'),
  (3, 'solicitud en revision'),
  (4, 'solicitud aprobada'),
  (5, 'rechazada');
