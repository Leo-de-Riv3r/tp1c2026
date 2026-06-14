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

No hay publicaciones, subastas, propuestas ni intercambios preseedeados — los crean los users desde el FE durante la demo.

## Levantar con Docker (stack completo, local)

Útil para **load tests** y desarrollo. Apunta a una Mongo local (no Atlas), tiene seed propio y no comparte estado con el cloud.

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

## Estado actual del proyecto

**Catálogo**: 991 figuritas estilo Panini en [`backend/seed/catalog.json`](backend/seed/catalog.json) que el contenedor `mongo-seed` inserta automáticamente en la colección `cards` al levantar el stack, junto con usuarios de prueba en la colección `users`

**Autenticación**: integrada vía JWT. Único endpoint `POST /api/auth/login` que detecta admin vs user según el `role` del User en Mongo (no hay endpoint admin separado). Todas las respuestas del API están en **inglés**. El filtro JWT valida además que el usuario del token siga existiendo en la base (`JwtAuthenticationFilter.java:53`).

**Interceptor de usuario**: `@ValidatesPathUser` en endpoints que reciben un `userId` como path variable — valida que el usuario exista antes de llegar al handler.

**Endpoints públicos**: `/api/auth*` y `/actuator*` no requieren token. El resto de los endpoints requieren un Bearer token válido.

### Usuarios del seed local

Mismos que el cloud (ver "Acceso al deploy" arriba). Stack y data son independientes — los users solo existen como duplicado para que el setup local sea autocontenido.

## Levantar Mongo localmente (sin Docker)

Si se quiere correr el BE local con `./mvnw spring-boot:run` y Mongo nativo en vez del compose:

**Requisitos:** [MongoDB Community Server](https://www.mongodb.com/try/download/community) instalado. El BE necesita Mongo en modo **replica set** (las transacciones `@Transactional` lo requieren)

**bash (Linux / macOS / Git Bash / WSL):**

```bash
# 1. Crear directorio de datos
mkdir -p /tmp/tacs-mongo

# 2. Arrancar mongod como replica set de 1 nodo (queda corriendo en foreground)
mongod --dbpath /tmp/tacs-mongo --port 27017 --replSet rs0 --bind_ip 127.0.0.1

# 3. En otra terminal: inicializar el replica set (solo la primera vez)
mongosh --eval 'rs.initiate({_id: "rs0", members: [{_id: 0, host: "localhost:27017"}]})'

# 4. Correr el seed (carga catálogo + usuarios)
mongosh "mongodb://localhost:27017/tacs_db?directConnection=true" --file backend/seed/seed.js

# 5. Levantar el BE apuntando a esa Mongo
cd backend
export SPRING_MONGODB_URI="mongodb://localhost:27017/tacs_db?directConnection=true"
./mvnw spring-boot:run
```

**PowerShell (Windows):**

```powershell
# 1. Crear directorio de datos
New-Item -ItemType Directory -Force -Path "$env:TEMP\tacs-mongo" | Out-Null

# 2. Arrancar mongod como replica set de 1 nodo (queda corriendo en foreground)
mongod --dbpath "$env:TEMP\tacs-mongo" --port 27017 --replSet rs0 --bind_ip 127.0.0.1

# 3. En otra terminal: inicializar el replica set (solo la primera vez)
mongosh --eval 'rs.initiate({_id: "rs0", members: [{_id: 0, host: "localhost:27017"}]})'

# 4. Correr el seed (carga catálogo + usuarios)
mongosh "mongodb://localhost:27017/tacs_db?directConnection=true" --file backend\seed\seed.js

# 5. Levantar el BE apuntando a esa Mongo
cd backend
$env:SPRING_MONGODB_URI = "mongodb://localhost:27017/tacs_db?directConnection=true"
.\mvnw.cmd spring-boot:run
```

> **Nota:** el seed es idempotente — si ya hay datos, no los duplica. Para resetear: parar mongodb, borrar `/tmp/tacs-mongo` o `%TEMP%\tacs-mongo` y empezar de nuevo

## Seedear MongoDB Atlas

Para popular un cluster M0 de Atlas (deploy cloud), correr el mismo `seed.js` apuntando al connection string del cluster. Requiere [`mongosh`](https://www.mongodb.com/try/download/shell) local y la IP outbound whitelisteada en Network Access.

Desde dentro de `backend/`:

```bash
mongosh "mongodb+srv://<user>:<password>@<cluster-host>/tacs_db?appName=<app>" --file seed/seed.js
```

El connection string completo (con password) queda solo en el password manager — nunca en el repo. El seed deja la base con 991 cards, 4 users de prueba, sin publications/auctions/proposals/exchanges (esos los crean los users desde el FE).

## Deploy en Render (gotchas)

El BE se deploya en Render free, contra Atlas M0. Notas para futuras corridas:

1. **Connection string**: usar `?retryWrites=true&w=majority` (formato canónico Atlas). Probamos primero `?authSource=admin&appName=...` y el driver Java se confundía intentando handshakes contra el database `local` aunque el user (`atlasAdmin@admin`) tuviera permisos. El formato con `retryWrites+w=majority` hace que el driver infiera bien el authSource y todo funciona.
2. **MongoHealthIndicator deshabilitado**: Spring Boot Actuator's MongoDB health check ejecuta `hello` contra el database `local`, que Atlas M0 restringe incluso para `atlasAdmin`. Sin el indicator deshabilitado, `/actuator/health` devuelve 503 → Render mata el container. Está apagado en `application.properties`. La app sigue conectando a Mongo normalmente para queries reales; el health-check verifica disk + app status sin tocar Mongo.
3. **Network Access en Atlas**: hay que agregar `0.0.0.0/0` porque Render free no tiene IPs estáticas. La protección queda en la auth user/password.
4. **Env vars críticas**: `SPRING_MONGODB_URI` (Atlas conn string), `JWT_SECRET` (32+ chars), `FRONTEND_URI` (URL del Netlify sin path ni trailing slash).
5. **`server.port=${PORT:8080}`**: Render asigna el puerto dinámicamente, Spring debe leerlo de `PORT`. Sin esto, "no service did not bind to port".
6. **Health Check Path**: `/actuator/health` (no `/healthz`, que es el default de Render).

## Visualizar la base (opcional)

Si se quiere inspeccionar los datos en Mongo, se debe instalar [`mongosh`](https://www.mongodb.com/try/download/shell) y/o [Compass](https://www.mongodb.com/try/download/compass)

**Instalar mongosh:**

| SO       | Comando                                                                |
|----------|------------------------------------------------------------------------|
| Windows  | `winget install MongoDB.Shell`                                         |
| macOS    | `brew install mongosh`                                                 |
| Ubuntu   | Ver [Install on Linux](https://www.mongodb.com/docs/mongodb-shell/install/) (requiere agregar el repo oficial de Mongo) |

Una vez instalado, conectarse a la base local:

```bash
mongosh "mongodb://localhost:27018/tacs_db"
```

**Compass:** descargá [acá](https://www.mongodb.com/try/download/compass), abrí, y conectá con la URI:

```
mongodb://localhost:27018
```

La base se llama `tacs_db`.

## Sobre el frontend

El servicio `frontend` del compose **no buildea desde código local**: pullea la imagen publicada en GHCR ([`ghcr.io/salometredici/tacs-2026-c1-fe@sha256:...`](https://github.com/salometredici/tacs-2026-c1-FE/pkgs/container/tacs-2026-c1-fe)). Se pueden generar las imágenes desde un workflow en el repositorio del FE de forma manual de ser necesario

Por eso, para correr el stack **no hace falta tener el frontend clonado**

Si se quiere trabajar con el FE en local (modificar código y verlo reflejado en docker), cloná el repo bajo el mismo padre del backend:

```
carpeta-padre/
├── tp1c2026/             ← este repo
└── tacs-2026-c1-FE/      ← repo del frontend
```

Y reemplazá el servicio `frontend` en `backend/docker-compose.yml` por:

```yaml
  frontend:
    build:
      context: ../../tacs-2026-c1-FE
      args:
        VITE_API_BASE_URL: http://localhost:8080
    ports:
      - "80:80"
    networks:
      - tacs-network
    depends_on:
      - backend
```

## Variables de entorno (backend)

| Variable             | Por defecto                          | Requerida | Descripción                                           |
|----------------------|--------------------------------------|-----------|-------------------------------------------------------|
| SPRING_MONGODB_URI   | —                                     | ✅        | URI de conexión a Mongo (ej: `mongodb://mongo:27017/tacs_db`) |
| JWT_SECRET           | —                                     | ✅        | Secreto para firmar JWTs (mínimo 32 caracteres)       |
| JWT_EXPIRATION       | 31536000000 (1 año)                   | ❌        | Duración del token en ms                              |
| FRONTEND_URI         | —                                     | ✅        | Origen(es) CORS permitidos (separados por coma)       |

> **Importante:** `JWT_SECRET` ya no tiene un valor por defecto de desarrollo. El backend falla al iniciar si no se configura. Creá un archivo `.env` en `backend/` basado en [`backend/.env.example`](backend/.env.example) antes de levantar el stack.
