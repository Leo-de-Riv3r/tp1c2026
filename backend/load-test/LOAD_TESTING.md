<!--
  Página pensada para la WIKI del repo (no es un README de carpeta).
  Pegar/linkear en: https://github.com/Leo-de-Riv3r/tp1c2026/wiki
  Cubre los dos enfoques de load testing y, sobre todo, cómo corren AISLADOS
  de la base que usan los profes.
-->

# Load testing

El proyecto tiene **dos enfoques de prueba de carga**, complementarios:

| Enfoque | Rol | Fortaleza |
|---|---|---|
| **Locust** (`locustfile.py`) | **Principal** | UI web en vivo (RPS, percentiles, errores), tool reconocida, evidencia commiteada (`results-sample.html`) |
| **Runner Node** (`src/`, ex-"k6") | Complemento | Modela **journeys de usuario realistas** (state machine con pesos, lecturas + escrituras), resumen de métricas por endpoint |

> El detalle de Locust (setup, tasks, troubleshooting, análisis) está en [load-test/README.md](README.md). Esta página cubre el panorama unificado y el **aislamiento**.

## Principio clave: las pruebas de carga NO tocan `tacs_db`

El load test corre sobre **su propia base, `tacs_loadtest_db`**, igual que la suite de tests usa `tacs_test_db`. La base que los profes usan para levantar la app (`tacs_db`) **queda intacta**: `docker compose up` (sin profile) no crea ni toca `tacs_loadtest_db` ni dispara ninguna prueba de carga.

Todo el stack de carga vive detrás del **profile `load-test`** en `backend/docker-compose.yml`.

## Correr el runner Node (turnkey, aislado)

Desde `backend/`:

```bash
docker compose --profile load-test up --build load-test
```

Eso levanta **solo la cadena de carga**: el mongo compartido + el seed de `tacs_loadtest_db` (catálogo + users base + 50 users `loadtest{i}@tacs.load`) + un **backend dedicado** (`backend-loadtest`, puerto `8081`, apuntado a `tacs_loadtest_db`) + el runner. No levanta el frontend ni el backend principal, y **no seedea `tacs_db`**.

Al terminar, el runner imprime el resumen de métricas (latencias avg/p95/max y % de fallos, global y por endpoint).

Parámetros por env (defaults entre paréntesis):

| Var | Default | Qué controla |
|---|---|---|
| `LOAD_TEST_USER_COUNT` | 50 | usuarios virtuales / users seedeados |
| `LOAD_TEST_DURATION` | 60 | duración del test (s) |
| `LOAD_TEST_START_USERS` | 5 | usuarios al inicio del ramp-up |
| `LOAD_TEST_MAX_USERS` | 50 | tope de usuarios concurrentes |

## Correr Locust (apuntando a la base aislada)

Para que Locust tampoco toque `tacs_db`, levantá el backend aislado y apuntá Locust a `:8081`:

```bash
# backend dedicado sobre tacs_loadtest_db (seedea solo)
docker compose --profile load-test up -d --build backend-loadtest

# Locust contra esa instancia
locust -f load-test/locustfile.py --host http://localhost:8081
# UI: http://localhost:8089
```

(El escenario de Locust es de solo-lectura, así que apuntarlo al `:8080` principal no modifica datos — pero usar `:8081` lo deja 100% aislado.)

## Decisión de diseño: ¿por qué el mongo va compartido?

El stack de carga **reutiliza el contenedor `mongo`** de la demo y solo cambia la **base** (`tacs_loadtest_db`). Razones:

- **Aísla los datos** (que es la prioridad: no tocar `tacs_db`) sin duplicar infraestructura.
- Reusa el `mongo-init` (replica set) que ya corre — **un contenedor menos** y sin re-inicializar.
- Para el alcance del TP, base separada + mongo compartido **alcanza y sobra**.

**Trade-off:** los datos están aislados, pero la **performance** se mide sobre el mismo mongod que la demo. No es un problema en la práctica (no se corre el load test durante la demo), pero si alguna vez querés medir sobre un mongo dedicado, ver abajo.

### Cómo separarlo en un mongo dedicado (si hiciera falta)

Un contenedor de mongo aparte arranca **vacío**, así que **hay que re-seedearlo** (no reusa nada del de la demo). Pasos:

1. Agregar bajo `profiles: [load-test]` un servicio `mongo-loadtest` (con `--replSet` + healthcheck, igual que `mongo`) y su `mongo-init-loadtest` (que corra `rs.initiate`).
2. Apuntar `mongo-seed-load-test` y `backend-loadtest` a ese host (`mongodb://mongo-loadtest:27017/tacs_loadtest_db`) y agregarlo a sus `depends_on`.
3. Correr igual: `docker compose --profile load-test up --build load-test`.

El costo es un contenedor + un init + el seed extra; el beneficio es performance totalmente aislada.

## Correr el runner contra un BE externo (ej. el deployado)

Para apuntar el runner a un backend que ya corre en otro lado (local fuera de Docker, o el cloud), está el compose autocontenido [load-test/docker-compose.yml](docker-compose.yml), que usa `host.docker.internal`:

```bash
cd load-test
API_BASE_URL=http://host.docker.internal:8080 docker compose up --build
```

> ⚠️ Si lo apuntás a un BE que usa `tacs_db` (o al cloud), el runner **sí** generará tráfico/escrituras sobre esa base. Usalo solo contra entornos descartables.
