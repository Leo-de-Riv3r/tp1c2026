# TACS 2026 C1 — Backend

Backend en Spring Boot + MongoDB para la plataforma de intercambio de figuritas

## Acceso al deploy (cloud)

La aplicación está corriendo en cloud sobre **tier free** de los tres proveedores (Atlas M0 + Render free + Netlify free):

| Componente | URL |
|---|---|
| Frontend (Netlify) | https://tacs-2026.netlify.app |
| Backend (Render)   | https://tacs-backend-2026.onrender.com |
| Database (Atlas M0) | acceso interno desde el BE — no exposed |

**Cómo probarlo**: ingresar desde el FE (`https://tacs-2026.netlify.app`). El BE solo acepta tráfico desde ese origen vía CORS — pegar a la URL del backend directo desde otro origen devuelve `403 Origin not allowed`.

### Cold start de Render free

El BE se "duerme" después de **15 min sin tráfico**. El primer request lo despierta y tarda **~30 segundos**. La pantalla de login en Netlify aparece rápido (es estático), pero el primer click "Iniciar sesión" puede colgar 30s la primera vez. Para evitar esto en una demo, pegarle a `https://tacs-backend-2026.onrender.com/actuator/health` un minuto antes de empezar.

### Usuarios disponibles

Todos comparten el mismo password: **`123456`**.

| Email                    | Rol   | Estado inicial                                                    |
|--------------------------|-------|-------------------------------------------------------------------|
| `peperacing@gmail.com`   | USER  | Tiene cards en colección (3× FWC1, 2× MEX1, 1× BRA3)              |
| `moniargento@gmail.com`  | USER  | Tiene cards en colección (1× FWC3, 2× ARG1, 3× BRA1, 1× ARG3, 1× MEX7) |
| `dfuseneco@outlook.com`  | USER  | Usuario "vacío" — sin colección. Para probar empty states         |
| `admin@mail.com`         | ADMIN | Usuario administrador (panel `/admin`)                            |

No hay publicaciones, subastas, propuestas ni intercambios preseedeados — los crean los users desde el FE durante la demo (a menos que se levante el modo demo, ver abajo).

## Levantar con Docker (stack completo, local)

Útil para **load tests** (ver [`backend/load-test/`](backend/load-test/)) y desarrollo. Apunta a una Mongo local (no Atlas), tiene seed propio y no comparte estado con el cloud.

**Requisitos:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) corriendo

Desde dentro de `backend/`:

```bash
docker compose up -d --build
```

Eso levanta los 5 servicios en orden:

1. **Mongo** — espera healthcheck
2. **mongo-init** — inicializa el replica set (espera a que Mongo esté listo, ejecuta `rs.initiate()` y termina)
3. **mongo-seed** — corre el script de seed (catálogo + usuarios de prueba). Termina y sale
4. **Backend** — espera a que el seed termine OK
5. **Frontend** — pullea la imagen publicada en GHCR (no hace falta clonar el repo)

| Servicio  | URL                                          |
|-----------|----------------------------------------------|
| Frontend  | http://localhost (puerto 80, default HTTP)   |
| Backend   | http://localhost:8080                        |
| MongoDB   | localhost:27018                              |
| Health    | http://localhost:8080/actuator/health        |

Para apagar todo:

```bash
docker compose down
```

Para resetear la base (borra el volumen y vuelve a seedear desde cero):

```bash
docker compose down -v
docker compose up -d --build
```

### Modo demo con actividad preseedeada (opcional)

Para una demo más rica (dashboard admin con curvas, perfiles de usuarios poblados, todos los flows del FE testeables sin crear datos a mano):

```bash
docker compose --profile demo up -d --build
```

El profile `demo` agrega el servicio `mongo-seed-full` que corre [`seed/seed_full.js`](backend/seed/seed_full.js) **después** del seed base. Carga:

- **6 publications** en los 3 estados (ACTIVA, FINALIZADA, CANCELADA).
- **3 auctions** en los 3 estados (ACTIVE, AWARDED, CANCELLED), incluyendo offers en los 4 estados (PENDING, ACCEPTED, REJECTED, CANCELLED).
- **6 proposals** en los 4 estados (PENDIENTE, ACEPTADA, RECHAZADA, CANCELADA).
- **3 exchanges** (2 originados por propuesta + 1 por subasta) con feedbacks parciales para mostrar el flow de calificaciones.
- **30 snapshots diarios** alineados con la actividad real → el dashboard admin muestra curvas con picos en los días con eventos.
- **Missing cards repartidas** con overlap entre users → el panel "Más buscadas" del admin tiene ranking.

Los `quantity` y `compromisedCount` de cada user reflejan exactamente las publications/auctions/proposals/offers activas — la base queda en un estado consistente, igual que si la actividad se hubiera hecho manualmente desde el FE.

**Importante**: el administrador NO participa de intercambios (solo visualiza el dashboard). Los flows de demo usan a Pepe / Moni / Dardo.

Para alternar entre modos:

```bash
docker compose down -v                          # borra el volumen
docker compose --profile demo up -d --build     # o sin --profile para modo base
```

## Variables de entorno

| Variable             | Por defecto                          | Requerida | Descripción                                           |
|----------------------|--------------------------------------|-----------|-------------------------------------------------------|
| `SPRING_MONGODB_URI` | —                                     | ✅        | URI de conexión a Mongo (ej: `mongodb://mongo:27017/tacs_db`) |
| `JWT_SECRET`         | —                                     | ✅        | Mínimo 32 caracteres. Legacy (ver Wiki)               |
| `JWT_EXPIRATION`     | 31536000000 (1 año)                   | ❌        | Legacy (ver Wiki)                                     |
| `SESSION_TTL`        | 3600000 (1 hora)                      | ❌        | Vida de la sesión en ms. Cada request la estira (sliding) |
| `FRONTEND_URI`       | —                                     | ✅        | Origen(es) CORS permitidos (separados por coma)       |

Crear un `.env` en `backend/` basado en [`backend/.env.example`](backend/.env.example) antes de levantar el stack.

## Más documentación en la Wiki

Lo siguiente vive en la [Wiki del repo](https://github.com/Leo-de-Riv3r/tp1c2026/wiki) para no inflar este README:

- **Decisiones de arquitectura e implementación**: autenticación con sesiones server-side en Mongo + TTL sliding, interceptores (`@RequiresOwnerOrAdmin`, `@ValidatesPathUser`), endpoints públicos, idioma de mensajes vs identificadores.
- **Levantar Mongo nativo (sin Docker)**: comandos bash y PowerShell para arrancar `mongod` como replica set de 1 nodo, inicializar `rs0`, correr el seed y levantar el BE con `./mvnw spring-boot:run`.
- **Seedear MongoDB Atlas**: cómo correr `seed.js` y `seed_full.js` contra un cluster M0 cloud.
- **Gotchas del deploy en Render + Atlas M0**: connection string que funciona, `MongoHealthIndicator` deshabilitado, Network Access `0.0.0.0/0`, env vars críticas, `server.port=${PORT:8080}`, health check path.
- **Visualizar la base**: instalación y uso de `mongosh` y MongoDB Compass para inspeccionar `tacs_db` local.
- **Build local del FE en lugar de usar la imagen GHCR**: snippet de `docker-compose.yml` para cuando se quiere modificar código del FE y verlo reflejado en el stack.
- **Load test**: motivación de elegir Locust, análisis del race condition que detectó, comparativa con JMeter, interpretación de métricas.
- **Uso de IA en el proyecto**: declaración requerida por el TP.
