ALTER TABLE solicitud_sede DROP CONSTRAINT fk_solicitud_sede_solicitud;

ALTER TABLE solicitud_sede
  ADD CONSTRAINT fk_solicitud_sede_solicitud
  FOREIGN KEY (solicitud_id) REFERENCES solicitud (id) ON DELETE CASCADE;
