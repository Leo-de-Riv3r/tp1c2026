# Setup local con Docker

## Prerequisitos

- Docker Desktop corriendo
- `mongosh` instalado y en el PATH ([descargar acá](https://www.mongodb.com/try/download/shell))
- Ambos repos clonados en la misma carpeta padre con estos nombres exactos:
  ```
  /alguna-carpeta/
  ├── tp1c2026/          ← repo backend
  └── tacs-2026-c1-FE/   ← repo frontend
  ```

## Primera vez (o al resetear la base)

Desde la carpeta padre (`/alguna-carpeta/`):

```bash
docker compose down -v
docker compose up --build -d
```

Esperá ~10 segundos a que Mongo esté listo, luego corré el seed desde la misma carpeta:

```bash
mongosh "mongodb://localhost:27018/tacs_db" --file tp1c2026/backend/seed/seed.js
```

> El seed es idempotente: si ya hay datos, los saltea sin pisar nada.

## Levantar sin resetear

```bash
docker compose up -d
```

## Servicios

| Servicio  | URL                   |
|-----------|-----------------------|
| Frontend  | http://localhost (puerto 80, default HTTP) |
| Backend   | http://localhost:8080 |
| MongoDB   | localhost:27018       |

## Bajar los containers

```bash
docker compose down
```

Para borrar también los datos de Mongo:

```bash
docker compose down -v
```
