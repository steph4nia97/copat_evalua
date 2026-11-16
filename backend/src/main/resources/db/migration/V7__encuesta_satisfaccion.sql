-- La evaluación anterior usaba cinco notas genéricas. D-SIG-10-01 las reemplaza.
-- Esas solicitudes vuelven a "enviada a cliente" para adjuntar la encuesta real.

DELETE FROM evidencia_revision;
DELETE FROM revision_departamento3;
DELETE FROM notificacion;
DELETE FROM solicitud_evaluacion;
DELETE FROM solicitud_pdf;

DELETE FROM solicitud_log
WHERE accion IN ('Solicitud enviada a revision', 'Revision registrada');

UPDATE solicitud
SET estado_id = (SELECT id FROM estado_solicitud WHERE nombre = 'solicitud enviada a cliente')
WHERE estado_id = (SELECT id FROM estado_solicitud WHERE nombre = 'solicitud en revision');

DROP TABLE solicitud_evaluacion;

CREATE TABLE solicitud_evaluacion (
  solicitud_id BIGINT PRIMARY KEY,
  nombre_encuestado VARCHAR(120) NOT NULL,
  empresa_encuesta VARCHAR(150) NOT NULL,
  cargo VARCHAR(120) NOT NULL,
  obra VARCHAR(200) NOT NULL,
  estado_obra VARCHAR(40) NOT NULL,
  comentarios VARCHAR(1000),
  gestion_administrativa SMALLINT NOT NULL,
  tiempos_entrega SMALLINT NOT NULL,
  estado_hormigon SMALLINT NOT NULL,
  flexibilidad SMALLINT NOT NULL,
  atencion_comercial SMALLINT NOT NULL,
  respuesta_reclamos SMALLINT NOT NULL,
  entrega_informacion SMALLINT NOT NULL,
  competitividad_precio SMALLINT NOT NULL,
  capacitacion_tecnica SMALLINT NOT NULL,
  percepcion_general SMALLINT NOT NULL,
  promedio NUMERIC(3, 2) NOT NULL,
  desempeno VARCHAR(40) NOT NULL,
  CONSTRAINT fk_evaluacion_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT chk_evaluacion_nombre CHECK (char_length(nombre_encuestado) BETWEEN 1 AND 120),
  CONSTRAINT chk_evaluacion_empresa CHECK (char_length(empresa_encuesta) BETWEEN 1 AND 150),
  CONSTRAINT chk_evaluacion_cargo CHECK (char_length(cargo) BETWEEN 1 AND 120),
  CONSTRAINT chk_evaluacion_obra CHECK (char_length(obra) BETWEEN 1 AND 200),
  CONSTRAINT chk_evaluacion_estado_obra CHECK (estado_obra IN ('Obra finalizada', 'Obra en construcción')),
  CONSTRAINT chk_evaluacion_comentarios CHECK (comentarios IS NULL OR char_length(comentarios) BETWEEN 1 AND 1000),
  CONSTRAINT chk_evaluacion_notas CHECK (
    gestion_administrativa BETWEEN 1 AND 5
    AND tiempos_entrega BETWEEN 1 AND 5
    AND estado_hormigon BETWEEN 1 AND 5
    AND flexibilidad BETWEEN 1 AND 5
    AND atencion_comercial BETWEEN 1 AND 5
    AND respuesta_reclamos BETWEEN 1 AND 5
    AND entrega_informacion BETWEEN 1 AND 5
    AND competitividad_precio BETWEEN 1 AND 5
    AND capacitacion_tecnica BETWEEN 1 AND 5
    AND percepcion_general BETWEEN 1 AND 5
  ),
  CONSTRAINT chk_evaluacion_promedio CHECK (promedio >= 1 AND promedio <= 5),
  CONSTRAINT chk_evaluacion_desempeno CHECK (char_length(desempeno) BETWEEN 1 AND 40)
);
