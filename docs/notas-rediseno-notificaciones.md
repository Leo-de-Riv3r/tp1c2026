# Rediseño de notificaciones — qué cambió

Branch `feat/notifications-redesign`, sobre tu commit (autoría preservada).

## De tu aporte se mantuvo
- Eventos `AuctionCreatedEvent` y `UserInterestedInAuctionEvent` (renombrado, antes "Action").
- Publicación de esos eventos en `AuctionService`/`ProposalService` + `createUserNotification`.
- `ScheduledNotification` y el cron `AuctionNotificationGenerator → checkScheduledNotifications`.
- Lead-times de "subasta por cerrar" (5 / 15 / 30 / 60 / 1440 min).
- Notificaciones embebidas en `User` y los repos de scheduled/global.

## Qué se modificó
- Notis de collection compartida → **embebidas por usuario** (`UserNotification`), con estado de
  lectura propio. Antes marcar leída mutaba el doc compartido por todos.
- Field injection → constructor injection.
- `@EventListener` → `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`.
- `ScheduledNotification.users` (`@DocumentReference List<User>`) → `List<String> userIds`.
- delete-then-send → deliver-then-delete; + dedupe por subasta y por carta.
- "Carta disponible" referencia a la carta, no a la publicación/subasta puntual.
- `NotificationPersistenceLevel` eliminado: el split global/propia es tagged-union (`globalId`).

Detalle de diseño en `docs/adr-notifications.md`. Suite: 139/139.
