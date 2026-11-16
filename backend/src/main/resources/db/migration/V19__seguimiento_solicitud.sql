ALTER TABLE solicitud ADD COLUMN seguimiento VARCHAR(40);

ALTER TABLE solicitud ADD CONSTRAINT chk_solicitud_seguimiento CHECK (
  seguimiento IS NULL OR seguimiento IN ('Recibida', 'En análisis', 'En gestión', 'Cerrada')
);

UPDATE solicitud
SET seguimiento = 'Recibida'
WHERE estado_id = (SELECT id FROM estado_solicitud WHERE nombre = 'solicitud en revision');

CREATE TABLE seguimiento_nota (
  id BIGSERIAL PRIMARY KEY,
  solicitud_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  estado VARCHAR(40) NOT NULL,
  nota VARCHAR(500) NOT NULL,
  creada_en TIMESTAMP NOT NULL,
  CONSTRAINT fk_seguimiento_nota_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud (id),
  CONSTRAINT fk_seguimiento_nota_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT chk_seguimiento_nota_estado CHECK (estado IN ('Recibida', 'En análisis', 'En gestión', 'Cerrada')),
  CONSTRAINT chk_seguimiento_nota_texto CHECK (char_length(btrim(nota)) BETWEEN 1 AND 500)
);

CREATE INDEX idx_seguimiento_nota_solicitud ON seguimiento_nota (solicitud_id, id);

INSERT INTO seguimiento_nota (solicitud_id, usuario_id, estado, nota, creada_en)
SELECT solicitud_id, usuario_id, 'Recibida', descripcion, actualizada_en
FROM revision_departamento3
WHERE char_length(btrim(descripcion)) BETWEEN 1 AND 500;

INSERT INTO solicitud_log (solicitud_id, fecha, hora, usuario_id, accion)
SELECT e.solicitud_id,
       e.recibida_en::date,
       e.recibida_en::time,
       COALESCE(
         (SELECT l.usuario_id
          FROM solicitud_log l
          WHERE l.solicitud_id = e.solicitud_id
            AND l.accion = 'Solicitud enviada a revision'
          ORDER BY l.id DESC
          LIMIT 1),
         s.creado_por_id),
       'Evaluación: ' || e.desempeno || ' · ' || replace(to_char(e.promedio, 'FM990.00'), '.', ',')
FROM solicitud_evaluacion e
JOIN solicitud s ON s.id = e.solicitud_id
WHERE NOT EXISTS (
  SELECT 1 FROM solicitud_log l
  WHERE l.solicitud_id = e.solicitud_id AND l.accion LIKE 'Evaluación:%'
);

INSERT INTO solicitud_log (solicitud_id, fecha, hora, usuario_id, accion)
SELECT s.id,
       COALESCE(e.recibida_en::date, CURRENT_DATE),
       COALESCE(e.recibida_en::time, CURRENT_TIME),
       s.creado_por_id,
       'Seguimiento: Recibida'
FROM solicitud s
LEFT JOIN solicitud_evaluacion e ON e.solicitud_id = s.id
WHERE s.seguimiento = 'Recibida'
  AND NOT EXISTS (
    SELECT 1 FROM solicitud_log l
    WHERE l.solicitud_id = s.id AND l.accion LIKE 'Seguimiento:%'
  );
