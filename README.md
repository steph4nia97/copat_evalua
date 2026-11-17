# Copat evalua

Monolito modular. Cubre Departamento 1 (solicitudes, clientes y envío de la encuesta) y Departamento 3 (seguimiento, métricas y reclamos).

## Requisitos

- Java 21
- Node.js 22
- Docker, para PostgreSQL 16

## Cómo partir

```bash
docker compose up -d
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

La aplicación queda en http://localhost:5173. La API escucha en el puerto 8080 y Vite la proxea en `/api`.

Base de datos local: `copat` / `copat` en `localhost:5432/copat_evalua`.

## Usuarios semilla

- Departamento 1: `semilla1@copat.cl` / `semilla1`
- Departamento 3: `semilla3@copat.cl` / `semilla3`

La contraseña se guarda con BCrypt. Un ingreso fallido responde siempre `Correo o contraseña incorrectos.`

## Departamento 1

El botón **Ingresar solicitud** guarda la evaluación con estado `solicitud creada`. La misma empresa, identificada por su RUT, puede tener varias evaluaciones si las fechas del período son distintas. El mismo RUT con el mismo inicio y el mismo término se rechaza.

Cada ingreso queda en la bitácora con fecha, hora y usuario de la sesión. Las reglas de nombre, RUT módulo 11, correo, teléfono chileno, fechas `dd/mm/aaaa`, plantilla y descripción se validan en el formulario, en la API y con restricciones de PostgreSQL.
