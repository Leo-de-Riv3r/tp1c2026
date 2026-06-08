# TACS 2026 C1 — Backend

Backend en Spring Boot + MongoDB para la plataforma de intercambio de figuritas

## Levantar con Docker (stack completo)

**Requisitos:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) corriendo

Desde dentro de `backend/`:

```bash
docker compose up -d --build
```

Eso levanta los 4 servicios en orden:

1. **Mongo** — espera healthcheck
2. **mongo-seed** — corre el script de seed (catálogo + usuario de prueba). Termina y sale
3. **Backend** — espera a que el seed termine OK
4. **Frontend** — pullea la imagen publicada en GHCR (no hace falta clonar el repo)

| Servicio  | URL                                          |
|-----------|----------------------------------------------|
| Frontend  | http://localhost (puerto 80, default HTTP)   |
| Backend   | http://localhost:8080                        |
| MongoDB   | localhost:27018                              |

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

**Catálogo**: todavía no encontramos una API pública que devuelva las figuritas del Mundial. Hay una con información de los jugadores y es posible encontrar el listado de las +900 figuritas — estamos evaluando combinar esta api y el listado para armar una propia. Mientras tanto, generamos un catálogo de 500 figuritas en [`backend/seed/catalog.json`](backend/seed/catalog.json) que el contenedor `mongo-seed` inserta automáticamente en la colección `cards` al levantar el stack, junto con usuarios de prueba en la colección `users`

**Autenticación**: integrada vía JWT. Único endpoint `POST /api/auth/login` que detecta admin vs user según el `role` del User en Mongo (no hay endpoint admin separado). El FE decodifica el claim `role` del JWT para decidir qué UI mostrar

## Usuarios de prueba

Todos los users del seed comparten el mismo password: **`123456`**

| Email                    | Rol   | Notas                                                                  |
|--------------------------|-------|------------------------------------------------------------------------|
| `peperacing@gmail.com`   | USER  | Tiene cards en colección (3x card_001, 2x card_005, 1x card_010) y missing cards |
| `moniargento@gmail.com`  | USER  | Tiene 2 publicaciones activas (card_003 y card_004)            |
| `dfuseneco@outlook.com`  | USER  | Usuario "vacío" — sin colección, faltantes, publicaciones ni propuestas. Para probar empty states |
| `admin@mail.com`         | ADMIN | Usuario de administración. Filtrado de listas de candidatos a trading  |

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
export SPRING_DATA_MONGODB_URI="mongodb://localhost:27017/tacs_db?directConnection=true"
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
$env:SPRING_DATA_MONGODB_URI = "mongodb://localhost:27017/tacs_db?directConnection=true"
.\mvnw.cmd spring-boot:run
```

> **Nota:** el seed es idempotente — si ya hay datos, no los duplica. Para resetear: parar mongodb, borrar `/tmp/tacs-mongo` o `%TEMP%\tacs-mongo` y empezar de nuevo

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

El servicio `frontend` del compose **no buildea desde código local**: pullea la imagen publicada en GHCR ([`ghcr.io/salometredici/tacs-2026-c1-fe:latest`](https://github.com/salometredici/tacs-2026-c1-FE/pkgs/container/tacs-2026-c1-fe)) (Si dejamos latest, será la imagen correspondiente al tag de la Entrega actual, no vamos a generar una imagen sobre código posterior a la misma hasta recibir la corrección, sino, vamos a dejar explícita la imagen en el docker-compose). Se pueden generar las imágenes desde un workflow en el repositorio del FE de forma manual de ser necesario

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

| Variable                    | Valor por defecto                     | Descripción                |
|-----------------------------|---------------------------------------|----------------------------|
| SPRING_DATA_MONGODB_URI     | mongodb://mongo:27017/tacs_db         | URI de conexión a Mongo    |
| MONGO_DATABASE              | tacs_db                               | Nombre de la base          |

PRUEBO EL WORKFLOW DE GITHUB
