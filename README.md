# Copat evalua

Monolito modular. Este corte cubre solo **Departamento 1**: ingreso de sesión, creación de una solicitud en estado `solicitud creada` y su bitácora.

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

## Usuario semilla

- Correo: `semilla1@copat.cl`
- Contraseña: `semilla1`

La contraseña se guarda con BCrypt. Un ingreso fallido responde siempre `Correo o contraseña incorrectos.`

## Departamento 1

El botón **Ingresar solicitud** guarda la evaluación con estado `solicitud creada`. La misma empresa, identificada por su RUT, puede tener varias evaluaciones si las fechas del período son distintas. El mismo RUT con el mismo inicio y el mismo término se rechaza.

Cada ingreso queda en la bitácora con fecha, hora y usuario de la sesión. Las reglas de nombre, RUT módulo 11, correo, teléfono chileno, fechas `dd/mm/aaaa`, plantilla y descripción se validan en el formulario, en la API y con restricciones de PostgreSQL.
