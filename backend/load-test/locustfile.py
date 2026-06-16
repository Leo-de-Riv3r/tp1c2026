"""
Load test del backend de TACS — primer approach con Locust.

Modela un usuario típico que loguea, navega publicaciones y subastas, y busca
cartas en el catálogo. Los pesos de cada task reflejan el patrón de uso real
(GET >>> POST), y el `on_start` autentica al user contra el endpoint real
para que el resto de los requests viajen con un sessionId válido.

Cómo correr (desde backend/):
    locust -f load-test/locustfile.py --host http://localhost:8080

Después abrir http://localhost:8089 y configurar:
    - Number of users:  50  (paralelos, ramp-up suave)
    - Spawn rate:       5   (5 users nuevos por segundo)
    - Run time:         1m  (60s es suficiente para mostrar la curva)

Para correr headless (sin UI) y exportar resultados a CSV:
    locust -f load-test/locustfile.py --host http://localhost:8080 \\
           --users 50 --spawn-rate 5 --run-time 1m --headless \\
           --csv results --html results.html
"""

import logging
import random
from locust import HttpUser, task, between, events

# Users seedeados en seed.js — todos comparten password "123456".
SEED_USERS = [
    {"email": "peperacing@gmail.com",  "password": "123456"},
    {"email": "moniargento@gmail.com", "password": "123456"},
    {"email": "dfuseneco@outlook.com", "password": "123456"},
]

# Términos típicos de búsqueda — refleja lo que tipearía un usuario que conoce
# el catálogo (Argentina, Brasil, jugadores, palabras parciales).
SEARCH_TERMS = ["Argentina", "Brasil", "Messi", "FWC", "México", "Francia", "Ron"]


class TacsUser(HttpUser):
    """
    Comportamiento de un usuario virtual: loguea una vez, después hace un mix
    de operaciones GET pesadas (publicaciones, subastas, búsqueda) con pausa
    entre 1 y 3 segundos (simula tiempo de lectura entre clicks).
    """

    wait_time = between(1, 3)

    def on_start(self):
        """
        Loguea contra el BE real y guarda el sessionId para los siguientes requests.
        Si el login falla NO abortamos el runner — el user virtual queda inactivo (no
        tendrá auth header → sus tasks pegarán 401 que se reflejan en métricas, pero
        el resto de los users siguen). Esto evita que 1 login lento mate la corrida.
        """
        creds = random.choice(SEED_USERS)
        self.user_id = None
        with self.client.post(
            "/api/auth/login",
            json={"email": creds["email"], "password": creds["password"]},
            catch_response=True,
            name="/api/auth/login",
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"Login falló ({resp.status_code}): {resp.text[:200]}")
                return
            try:
                data = resp.json()
            except Exception as e:
                resp.failure(f"Login response no es JSON: {e}")
                return
            token = data.get("token")
            if not token:
                resp.failure(f"Login OK pero sin token: {data}")
                return
            self.client.headers["Authorization"] = f"Bearer {token}"
            self.user_id = data.get("user", {}).get("id")

    @task(4)
    def list_publications(self):
        """GET paginado de publicaciones activas — el endpoint más usado por la UI."""
        self.client.get(
            "/api/publications?page=1&per_page=10",
            name="/api/publications [GET]",
        )

    @task(3)
    def list_auctions(self):
        """GET paginado de subastas activas."""
        self.client.get(
            "/api/auctions?page=1&per_page=10",
            name="/api/auctions [GET]",
        )

    @task(2)
    def search_cards(self):
        """Búsqueda full-text sobre el catálogo (índice de texto en Mongo)."""
        term = random.choice(SEARCH_TERMS)
        self.client.get(
            f"/api/cards/search?q={term}&page=1&per_page=10",
            name="/api/cards/search?q=[...] [GET]",
        )

    @task(1)
    def get_my_user(self):
        """GET del user propio — chequeo de auth + sliding TTL de la sesión."""
        if not getattr(self, "user_id", None):
            return
        self.client.get(
            f"/api/users/{self.user_id}",
            name="/api/users/{id} [GET]",
        )


@events.quitting.add_listener
def _exit_with_nonzero_on_high_error_rate(environment, **_kwargs):
    """
    Falla la corrida si supera 1% de error rate o p95 > 3s.
    Hace que el script exitee con código != 0 — útil si después se
    integra en un workflow CI que decida pasar/fallar.
    """
    stats = environment.stats.total
    if stats.num_requests == 0:
        logging.warning("No se ejecutó ningún request.")
        environment.process_exit_code = 1
        return
    fail_ratio = stats.num_failures / stats.num_requests
    p95 = stats.get_response_time_percentile(0.95)
    if fail_ratio > 0.01:
        logging.error(f"Error rate {fail_ratio:.2%} > 1%")
        environment.process_exit_code = 1
    elif p95 > 3000:
        logging.error(f"p95 latency {p95:.0f}ms > 3000ms")
        environment.process_exit_code = 1
    else:
        logging.info(f"OK — error rate {fail_ratio:.2%}, p95 {p95:.0f}ms")
