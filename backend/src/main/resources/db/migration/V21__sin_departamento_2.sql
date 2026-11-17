UPDATE usuario SET departamento_id = 1 WHERE departamento_id = 2;

UPDATE registro_actividad SET departamento_id = 1 WHERE departamento_id = 2;

UPDATE solicitud SET departamento_id = 1 WHERE departamento_id = 2;

DELETE FROM notificacion n
WHERE n.departamento_id = 2
  AND EXISTS (
    SELECT 1 FROM notificacion o
    WHERE o.solicitud_id = n.solicitud_id AND o.departamento_id = 1);

UPDATE notificacion SET departamento_id = 1 WHERE departamento_id = 2;

ALTER TABLE departamento DROP CONSTRAINT chk_departamento_id;

DELETE FROM departamento WHERE id = 2;

ALTER TABLE departamento ADD CONSTRAINT chk_departamento_id CHECK (id IN (1, 3, 4));
