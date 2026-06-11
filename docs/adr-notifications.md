# ADR — Rediseño del sistema de notificaciones

Estado: **Aceptado** · Fecha: 2026-06-11 · Scope: **Backend**

## Contexto

El sistema presentado guardaba notificaciones como **documentos compartidos** entre
usuarios: marcar una como leída mutaba el doc que todos referenciaban → "leída por uno,
leída por todos". Además la US#11 (Alertas) tenía `@Async` sin efecto (falta `@EnableAsync`)
y el cron disparaba de forma rígida.

Queremos: estado de lectura **por usuario**, generación **simple** de anuncios a todos,
distribución **eficiente**, y un cron más flexible.

## Decisiones

1. **Notis propias embebidas en el `User`**, como cola **FIFO con tope 20**. El estado
   `READ/UNREAD` vive en el item embebido → marcar leída nunca afecta a otros.
   - Eviction: al pasar de 20 se descarta la más vieja **leída**; si están todas sin leer,
     cae la más vieja. (Limitación documentada: más de 20 sin leer ⇒ se pierden las viejas.)

2. **Globales stateless** en su propia collection `global_notifications`. Se guardan una
   sola vez. En la cola del user se inserta una **referencia** (`globalId`), y el estado de
   lectura vive en esa referencia (no en la global). Caducan solas vía `validUntil`.

3. **Item polimórfico = tagged-union, no herencia.** Una sola clase `UserNotification` con
   `data` (inline, propia) **o** `globalId` (ref a global); el discriminador es un null-check.
   Se evita una jerarquía con `@TypeAlias` (que el profe marcó como frágil).

4. **Contenido = `NotificationData` tipado** (`type`, `message`, `referenceId`, `link?`),
   solo **semántica**. La presentación (texto / link / card rica) la resuelve el **FE** según
   `type`. No se guarda HTML en el backend (XSS / acoplamiento de capas).

5. **Distribución = Pub/Sub (Spring events) + `@TransactionalEventListener(AFTER_COMMIT)` +
   `@Async`** (con `@EnableAsync`). El fan-out corre fuera del hilo del request y solo si la
   operación commiteó. Esto además cierra el bloqueante D₂.
   - **Fan-out de globales/broadcast vía bulk `$push`**: un solo `updateMany(<audiencia>,
     { $push: { notifications: { $each:[ref], $slice:-20 } } })` — Mongo distribuye
     server-side y `$slice:-20` aplica el tope atómicamente. Costo = I/O, no CPU.
   - **Sectorización** (futuro): el filtro de audiencia del `updateMany` es el punto de
     extensión; default = todos.

6. **Card disponible: dedupe.** No se manda otra "carta X disponible" si el user ya tiene una
   **sin leer** de esa carta. La noti referencia a la **carta** (no a la publicación puntual)
   para evitar links stale y permitir el dedupe. *(El FE necesita una vista "ver dónde está
   disponible" — ítem transversal, va al handoff de FE.)*

7. **Cron flexible**: `ScheduledNotification` con `scheduledTime`; el cron barre las vencidas
   cada pocos minutos (no cada hora fija). Resiliente a restarts (vive en Mongo).

## Consecuencias

- ✅ Read-state correcto por usuario; badge = `count(UNREAD)` en la cola embebida.
- ✅ Lectura uniforme (todo sale de la cola de ≤20); globales se resuelven con un batch-fetch.
- ✅ Cierra el bloqueante `@Async`/`@EnableAsync` (D₂) y aplica `AFTER_COMMIT`.
- ⚠️ Historial **lossy** más allá de 20 (aceptado; apps que necesitan historial completo usan
  una collection paginada en vez de embeber).
- ⚠️ Late-joiners no reciben globales previas a su registro (esperado para "anuncios").

## Reutilización

Se parte del aporte de **Lautaro Moyano** (commit cherry-pickeado, autoría preservada):
eventos (`AuctionCreatedEvent`, `UserInterestedInAuctionEvent`), enum
`NotificationPersistenceLevel{GLOBAL,USER}` y el concepto `ScheduledNotification`.
Adaptado a: constructor injection, `AFTER_COMMIT`+`@EnableAsync`, cola tope-20 y la ref
polimórfica a global.
