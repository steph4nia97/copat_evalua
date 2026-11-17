ALTER TABLE usuario ADD COLUMN cargo VARCHAR(80);

UPDATE usuario SET cargo = 'Por asignar' WHERE cargo IS NULL;

ALTER TABLE usuario ALTER COLUMN cargo SET NOT NULL;

ALTER TABLE usuario ADD CONSTRAINT chk_usuario_cargo CHECK (
  char_length(cargo) BETWEEN 1 AND 80
);
