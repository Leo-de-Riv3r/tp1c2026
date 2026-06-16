# Load Test — primer approach con Locust

> ⚠️ **Esto es un primer approach** para cumplir el RNF-7 del TP (*"La aplicación debe soportar un load test, se utilizará alguna tool como Vegeta, Wrk, etc."*). Para la próxima entrega planeamos: portar el escenario a **JMeter** (en paralelo, no reemplazo), instrumentar el BE con **OpenTelemetry** para ver qué método se vuelve lento bajo carga, y agregar más tasks de escritura (`POST /publications`, `POST /proposals`, bids de subasta). El análisis detallado de resultados y la motivación de la elección de tool están en la [Wiki](https://github.com/Leo-de-Riv3r/tp1c2026/wiki).

## Tool elegida: Locust

- Los profes la nombraron como una de las válidas (junto a Vegeta, Wrk, JMeter, etc.).
- Scripts en Python (los del equipo ya manejan el lenguaje).
- UI web en vivo (`http://localhost:8089`) con gráficos de RPS, percentiles y errores.
- Setup mínimo: `pip install locust`.

## Setup local

```bash
# 1. Python 3.10+
python --version

# 2. Locust
pip install locust
```

## Cómo correr

**Pre-requisito**: el stack del BE arriba en `localhost:8080`:

```bash
cd backend
docker compose --profile demo up -d --build    # con datos demo (recomendado)
```

### Modo interactivo (UI)

```bash
locust -f load-test/locustfile.py --host http://localhost:8080
```

Abrir [`http://localhost:8089`](http://localhost:8089) y configurar:

| Parámetro       | Valor sugerido |
|-----------------|----------------|
| Number of users | `20`           |
| Spawn rate      | `2`            |
| Run time        | `1m`           |

### Modo headless (exporta CSV + HTML)

```bash
locust -f load-test/locustfile.py --host http://localhost:8080 \
       --users 20 --spawn-rate 2 --run-time 1m --headless \
       --csv results --html results.html
```

Genera `results_stats.csv`, `results_failures.csv` y `results.html` en `backend/`.

## Qué simula el script

Cada usuario virtual loguea con uno de los 3 users seedeados (Pepe / Moni / Dardo) y entra en un loop con pausa de 1-3 segundos entre acciones:

| Task                  | Peso | Endpoint                                |
|-----------------------|------|-----------------------------------------|
| Listar publicaciones  | 4    | `GET /api/publications?page=1`          |
| Listar subastas       | 3    | `GET /api/auctions?page=1`              |
| Buscar cartas         | 2    | `GET /api/cards/search?q=...`           |
| GET user propio       | 1    | `GET /api/users/{id}`                   |

Un listener interno (`@events.quitting`) hace exit con código != 0 si error rate > 1% o p95 > 3s — útil para integrar en CI.

## Evidencia de corrida real

[`results-sample.html`](results-sample.html) — corrida headless de 1 min, 566 requests, **0 failures**, p95 130ms. Abrir directo en el browser para ver gráficos y métricas por endpoint.

## Troubleshooting

**Errores 401 en publications/auctions/search**: el login de ese user virtual falló → no tiene `Authorization` header. Mirar primero la fila de `/api/auth/login`.

**Logins lentos (~150ms)**: Spring Security usa BCrypt cost=10, que es CPU-bound a propósito (defensa contra fuerza bruta). Esperable.

**Corrida termina sin requests**: el runner se quedó sin VUs activos. Verificar `docker compose ps` que todo esté `healthy`.

---

Más detalle (motivación de Locust, análisis del finding del race condition, interpretación de métricas, comparativa con JMeter) en la [Wiki del repo](https://github.com/Leo-de-Riv3r/tp1c2026/wiki).
