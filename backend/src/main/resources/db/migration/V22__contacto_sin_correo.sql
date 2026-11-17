ALTER TABLE contacto DROP CONSTRAINT uq_contacto_rut_correo;

CREATE UNIQUE INDEX uq_contacto_rut_correo
  ON contacto (rut, correo)
  WHERE correo <> 'No aplica';

CREATE UNIQUE INDEX uq_contacto_sin_correo
  ON contacto (rut, lower(nombre))
  WHERE correo = 'No aplica';

ALTER TABLE solicitud DROP CONSTRAINT chk_solicitud_correo;
ALTER TABLE solicitud ADD CONSTRAINT chk_solicitud_correo CHECK (
  correo ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'
  OR correo = 'No aplica'
);

ALTER TABLE solicitud DROP CONSTRAINT chk_solicitud_telefono;
ALTER TABLE solicitud ADD CONSTRAINT chk_solicitud_telefono CHECK (
  telefono IS NULL
  OR telefono ~ '^(56)?[2-9][0-9]{8}$'
  OR telefono = 'No aplica'
);
