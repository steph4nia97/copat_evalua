-- Flexibilidad y respuesta a reclamos pueden quedar sin nota.
-- NULL significa "No aplica" y no entra al promedio.

ALTER TABLE solicitud_evaluacion
  ALTER COLUMN flexibilidad DROP NOT NULL,
  ALTER COLUMN respuesta_reclamos DROP NOT NULL;

ALTER TABLE solicitud_evaluacion DROP CONSTRAINT chk_evaluacion_notas;

ALTER TABLE solicitud_evaluacion ADD CONSTRAINT chk_evaluacion_notas CHECK (
  gestion_administrativa BETWEEN 1 AND 5
  AND tiempos_entrega BETWEEN 1 AND 5
  AND estado_hormigon BETWEEN 1 AND 5
  AND (flexibilidad IS NULL OR flexibilidad BETWEEN 1 AND 5)
  AND atencion_comercial BETWEEN 1 AND 5
  AND (respuesta_reclamos IS NULL OR respuesta_reclamos BETWEEN 1 AND 5)
  AND entrega_informacion BETWEEN 1 AND 5
  AND competitividad_precio BETWEEN 1 AND 5
  AND capacitacion_tecnica BETWEEN 1 AND 5
  AND percepcion_general BETWEEN 1 AND 5
);
