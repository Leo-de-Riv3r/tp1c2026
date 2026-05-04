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

**Catálogo**: todavía no encontramos una API pública que devuelva las figuritas del Mundial. Hay una con información de los jugadores y es posible encontrar el listado de las +900 figuritas — estamos evaluando combinar esta api y el listado para armar una propia. Mientras tanto, generamos un catálogo de 500 figuritas en [`backend/seed/catalog.json`](backend/seed/catalog.json) que el contenedor `mongo-seed` inserta automáticamente en la colección `cards` al levantar el stack, junto con un usuario de prueba en la colección `users`

**Autenticación**: el módulo de auth todavía no está integrado (en progreso). Mientras tanto, el frontend usa un mock que apunta al ID del usuario seedeado para que ambos coincidan y se puedan ejecutar las operaciones básicas contra la instancia de Mongo

## Visualizar la base (opcional)

Si se quiere inspeccionar los datos en Mongo, se debe instalar [`mongosh`](https://www.mongodb.com/try/download/shell) y/o [Compass](https://www.mongodb.com/try/download/compass).

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

El servicio `frontend` del compose **no buildea desde código local**: pullea la imagen publicada en GHCR ([`ghcr.io/salometredici/tacs-2026-c1-fe:latest`](https://github.com/salometredici/tacs-2026-c1-FE/pkgs/container/tacs-2026-c1-fe)). Cada merge a `main` del [repo del frontend](https://github.com/salometredici/tacs-2026-c1-FE) dispara un workflow de GitHub Actions que rebuildea y republica la imagen

Por eso, para correr el stack **no hace falta tener el frontend clonado**.

Si querés trabajar con el frontend en local (modificar código y verlo reflejado en docker), cloná el repo como hermano del backend:

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
