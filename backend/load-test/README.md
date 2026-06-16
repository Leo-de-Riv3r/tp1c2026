# Load Test — primer approach con Locust

> ⚠️ **Esto es un primer approach** para cumplir el RNF-7 del TP (*"La aplicación debe soportar un load test, se utilizará alguna tool como Vegeta, Wrk, etc."*). Sirve como base testable y como evidencia de que el RNF está cubierto. El compañero del equipo (Lautaro) puede preferir migrar a **JMeter** o agregar **OpenTelemetry** para observabilidad detallada — esta carpeta es punto de partida, no la versión final.

## Por qué Locust

Los profes nombraron Locust como una de las tools válidas. Se eligió porque:

- **UI web en vivo** (`http://localhost:8089`) que muestra curvas de RPS, percentiles de latencia y errores — útil para screenshots de defensa.
- Scripts en **Python** (el código de cada task es legible sin learning curve adicional).
- Setup mínimo: `pip install locust` y listo, no requiere Docker ni Java extra.
- Soporte nativo para autenticación con headers persistentes por usuario virtual.

## Setup local

```bash
# 1. Tener Python 3.10+ instalado
python --version

# 2. Instalar Locust
pip install locust

# 3. Verificar
locust --version
```

## Cómo correr

**Pre-requisito**: el stack del backend tiene que estar arriba en `localhost:8080`. La forma típica:

```bash
cd backend
docker compose --profile demo up -d --build    # con datos demo (recomendado)
# o sin --profile para arrancar con base limpia
```

### Modo interactivo (con UI)

```bash
# Desde la carpeta backend/
locust -f load-test/locustfile.py --host http://localhost:8080
```

Abrir [`http://localhost:8089`](http://localhost:8089) y configurar:

| Parámetro          | Valor sugerido | Por qué                                                |
|--------------------|----------------|--------------------------------------------------------|
| Number of users    | `50`           | Carga concurrente realista para un free tier           |
| Spawn rate         | `5`            | 5 users nuevos por segundo (ramp-up suave de 10s)      |
| Host               | `http://localhost:8080` | Pre-cargado por el flag `--host`              |
| Run time           | `1m`           | Suficiente para mostrar la curva estable                |

### Modo headless (sin UI, exporta resultados)

```bash
locust -f load-test/locustfile.py --host http://localhost:8080 \
       --users 50 --spawn-rate 5 --run-time 1m --headless \
       --csv results --html results.html
```

Genera:

- `results_stats.csv` — un row por endpoint con count/avg/min/max/p50/p95.
- `results_failures.csv` — detalle de cada fallo.
- `results.html` — reporte HTML standalone (ideal para adjuntar a la entrega).

## Qué simula el script

Cada usuario virtual:

1. **`on_start`**: loguea con uno de los 3 usuarios seedeados (`peperacing`, `moniargento`, `dfuseneco`) y guarda el `sessionId` en los headers.
2. Loop con pausa **1-3 segundos** entre acciones (simula lectura humana). Mix de tasks pesado en GETs (≈90% del tráfico real):

| Task                      | Peso | Endpoint                                |
|---------------------------|------|-----------------------------------------|
| Listar publicaciones      | 4    | `GET /api/publications?page=1`          |
| Listar subastas           | 3    | `GET /api/auctions?page=1`              |
| Buscar cartas             | 2    | `GET /api/cards/search?q=...`           |
| GET user propio           | 1    | `GET /api/users/{id}` — toca sliding TTL |

3. Cuando termina la corrida, un listener interno (`@events.quitting`) chequea:
   - **Error rate** debe ser ≤ 1%.
   - **Latencia p95** ≤ 3000ms.

   Si alguno falla, Locust exitea con código != 0 — útil para integrar en CI.

## Troubleshooting

**Si ves muchos fallos en `/api/auth/login` con latencia alta (~1s)**:
- Spring Security usa **BCrypt** para hashear passwords con cost factor 10 — eso es **CPU-bound y lento a propósito** (defensa contra fuerza bruta). Con 50 logins concurrentes, la JVM single-core que corre el BE en compose se satura.
- **Fix**: bajar la concurrencia inicial. En el UI, configurar `Number of users = 20` y `Spawn rate = 2`. Eso ramp-uppea más suave y deja al BCrypt respirar.

**Si ves errores 401 en otras tasks (publications/auctions/search)**:
- Significa que el login del user virtual falló. El user queda sin `Authorization` header → 401 en todo lo demás. Mirar primero la fila de `/api/auth/login` para entender la raíz.

**Si la corrida termina muy rápido (5-10 requests totales)**:
- El runner quedó sin VUs activos. Verificar que Docker no se haya parado a mitad de camino (`docker compose ps`).

## Cómo interpretar los resultados

| Métrica            | Verde      | Amarillo    | Rojo          |
|--------------------|------------|-------------|---------------|
| RPS                | > 30/s     | 10-30/s     | < 10/s        |
| Latencia p95       | < 500ms    | 500ms-2s    | > 2s          |
| Error rate         | < 0.1%     | 0.1-1%      | > 1%          |

Sobre **free tier**: el BE corre en Render free → no esperes RPS de producción real. El objetivo del load test acá es **detectar regresiones** (si un cambio mete latencia 10x, lo vas a ver) y **demostrar que la app soporta concurrencia** (no se cae con 50 users en simultáneo).

## Próximos pasos / mejoras posibles

- **JMeter**: si Lautaro prefiere herramientas con tradición más industrial, se puede portar el escenario a JMeter (`.jmx`) y mantener ambos.
- **OpenTelemetry**: instrumentar el BE con OTel para emitir traces durante la carga → permite ver qué query Mongo se vuelve lenta o qué método tiene contención. Setup ≈ 2-3h.
- **Más tasks**: hoy el load test sólo modela usuarios "lectores". Se puede agregar:
  - `POST /api/publications` (publicar una figurita) — escenario de escritura.
  - `POST /api/proposals` (hacer propuesta) — más representativo del marketplace.
  - `POST /api/auctions/{id}/bids` (ofertar en subasta) — concurrencia + optimistic locking real.
- **Escenarios con thresholds más estrictos** para CI.
