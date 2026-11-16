-- El ingreso de solicitudes y el envío de la encuesta son el mismo departamento.

UPDATE usuario
SET departamento_id = 1
WHERE departamento_id = 2;

UPDATE registro_actividad
SET departamento_id = 1
WHERE departamento_id = 2;
