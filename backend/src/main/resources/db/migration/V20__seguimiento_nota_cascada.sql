ALTER TABLE seguimiento_nota DROP CONSTRAINT fk_seguimiento_nota_solicitud;

ALTER TABLE seguimiento_nota
  ADD CONSTRAINT fk_seguimiento_nota_solicitud
  FOREIGN KEY (solicitud_id) REFERENCES solicitud (id) ON DELETE CASCADE;
