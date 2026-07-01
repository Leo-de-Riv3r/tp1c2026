# Setup local con Docker

> El paso a paso completo (con modo demo, usuarios de prueba, y troubleshooting)
> vive en el [README del repo](../README.md). Para detalles operativos
> (Mongo nativo sin Docker, gotchas de Render, visualizar la base) ver la
> [Wiki](https://github.com/Leo-de-Riv3r/tp1c2026/wiki).

## TL;DR

Desde dentro de `backend/`, con Docker Desktop corriendo:

```bash
docker compose up -d --build
```

Eso levanta Mongo (con replica set), corre el seed automáticamente y arranca el BE + FE.
No hace falta clonar el repo del FE: viene como imagen pulleada de GHCR.

| Servicio  | URL                                          |
|-----------|----------------------------------------------|
| Frontend  | http://localhost                             |
| Backend   | http://localhost:8080                        |
| MongoDB   | localhost:27018                              |
| Health    | http://localhost:8080/actuator/health        |

Para resetear la base:

```bash
docker compose down -v
docker compose up -d --build
```

Los usuarios base quedan solo con figuritas (colección + faltantes); las publicaciones, subastas, propuestas, intercambios y notificaciones se crean a mano desde el FE.
