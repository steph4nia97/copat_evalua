UPDATE estado_solicitud
SET nombre = 'solicitud enviada a cliente'
WHERE nombre = 'solicitud enviada';

UPDATE estado_solicitud
SET nombre = 'solicitud en revision'
WHERE nombre = 'solicitud enviada a revision';
