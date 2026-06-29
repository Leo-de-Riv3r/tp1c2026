package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.user.input.*;
import com.tacs.tp1c2026.entities.dto.user.output.CollectionCardDto;
import com.tacs.tp1c2026.entities.dto.user.output.CollectionCardResult;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.user.output.MissingCardDto;
import com.tacs.tp1c2026.entities.dto.user.output.NotificationDto;
import com.tacs.tp1c2026.entities.dto.user.output.SuggestionResult;
import com.tacs.tp1c2026.entities.dto.user.output.UserDto;
import com.tacs.tp1c2026.config.RequiresOwnerOrAdmin;
import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.config.ValidatesPathUser;
import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.services.NotificationService;
import com.tacs.tp1c2026.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserService userService;
    private final NotificationService notificationService;

    public UsersController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    /**
     * Devuelve todos los users registrados como DTOs. Para la vista de admin.
     * @return lista de users como {@link UserDto}
     */
    @GetMapping
    @RequiresRole("ADMIN")
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.getAll().stream()
                .map(UserDto::from)
                .toList());
    }

    /**
     * Devuelve un user por su ID de Mongo. Solo accesible para el propio user o ADMIN.
     * @param id ID del user
     * @return el user como {@link UserDto}, o 404 si no se encuentra
     */
    @GetMapping("/{id}")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<UserDto> getById(@PathVariable String id) throws NotFoundException {
        return ResponseEntity.ok(UserDto.from(userService.getById(id)));
    }

    /* Collection endpoints */

    /**
     * Devuelve la colección de figuritas del user.
     * @param id ID del user
     * @return lista de figuritas en su colección
     */
    @GetMapping("/{id}/collection")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<List<CollectionCardDto>> getCollection(@PathVariable String id) throws NotFoundException {
        return ResponseEntity.ok(userService.getUserCardCollection(id).stream()
                .map(CollectionCardDto::from)
                .toList());
    }

    /**
     * Agrega una figurita a la colección del user. Si la figurita ya está, incrementa su cantidad.
     * Devuelve 201 si la figurita se agregó por primera vez, 200 si se incrementó la cantidad.
     * @param id ID del user
     * @param request body con el ID de la figurita a agregar
     * @return el {@link CollectionCardDto} actualizado
     */
    @PostMapping("/{id}/collection")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<CollectionCardDto> addToCollection(
            @PathVariable String id,
            @Valid @RequestBody AddToCollectionRequest request) throws NotFoundException, NotFoundException, NotFoundException {
        CollectionCardResult result = userService.addCardToUserCollection(id, request.cardId(), request.quantity());
        return ResponseEntity
            .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
            .body(CollectionCardDto.from(result.card()));
    }

    /**
     * Decrementa en uno la cantidad de una figurita en la colección del user.
     * Si la cantidad llega a cero, elimina la entrada por completo.
     * @param id ID del user
     * @param cardId ID de la figurita a decrementar
     */
    @PatchMapping("/{id}/collection/{cardId}")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<Void> decrementFromCollection(
            @PathVariable String id,
            @PathVariable String cardId) throws ConflictException, NotFoundException, NotFoundException, NotFoundException {
        userService.decrementFromCollection(id, cardId);
        return ResponseEntity.noContent().build();
    }

    /* Missing cards endpoints */

    /**
     * Devuelve la lista de figuritas que el user está buscando.
     * @param id ID del user
     * @return lista de figuritas faltantes
     */
    @GetMapping("/{id}/missing-cards")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<List<MissingCardDto>> getMissingCards(@PathVariable String id) throws NotFoundException {
        return ResponseEntity.ok(userService.getUserMissingCards(id).stream()
                .map(MissingCardDto::from)
                .toList());
    }

    /**
     * Marca una figurita como faltante para el user. No hace nada si ya está en la lista.
     * @param id ID del user
     * @param request body con el ID de la figurita a marcar como faltante
     * @return 201 con la entrada {@link MissingCardDto} creada
     */
    @PostMapping("/{id}/missing-cards")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<MissingCardDto> addMissingCard(
            @PathVariable String id,
            @Valid @RequestBody AddMissingCardRequest request) throws NotFoundException, NotFoundException {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(MissingCardDto.from(userService.addMissingCard(id, request.cardId())));
    }

    /**
     * Saca una figurita de la lista de faltantes del user.
     * @param id ID del user
     * @param cardId ID de la figurita a quitar
     */
    @DeleteMapping("/{id}/missing-cards/{cardId}")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<Void> removeMissingCard(
            @PathVariable String id,
            @PathVariable String cardId) throws NotFoundException, NotFoundException {
        userService.removeFromMissingCards(id, cardId);
        return ResponseEntity.noContent().build();
    }

    /* Suggestions endpoint */

    /**
     * Devuelve las sugerencias de intercambio para un user.
     * Las sugerencias las computa periódicamente el SuggestionGenerator agendado.
     * Cada sugerencia apunta a una publicación o subasta activa específica (de otro user
     * con perfil similar) cuya figurita matchea con un faltante del user.
     * @param id ID del user
     * @return lista de {@link SuggestionResult}
     */
    @GetMapping("/{id}/suggestions")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<List<SuggestionResult>> getSuggestions(@PathVariable String id) throws NotFoundException {
        return ResponseEntity.ok(
            userService.getUserSuggestions(id).stream()
                .map(SuggestionResult::from)
                .toList()
        );
    }

    /* Notification endpoints */

    /**
     * Notificaciones del user, paginadas y filtradas por {@code status} (READ / UNREAD).
     * Solo el dueño o un admin.
     */
    @GetMapping("/{id}/notifications")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<PaginationDtoOutput<NotificationDto>> getNotifications(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer per_page,
            @RequestParam(required = true) NotificationStatus status) {
        Page<NotificationDto> result = notificationService.getNotificationsForUser(id, status, page, per_page);
        return ResponseEntity.ok(new PaginationDtoOutput<>(result.getContent(), result.getNumber() + 1, result.getTotalPages()));
    }

    /** Marca todas las notificaciones del user como leídas. Solo el dueño o un admin. */
    @PutMapping("/{id}/notifications/read")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<Void> markAllNotificationsAsRead(@PathVariable String id) {
        notificationService.markAllUserNotificationsAsRead(id);
        return ResponseEntity.noContent().build();
    }

    /** Marca una notificación puntual como leída. Solo el dueño o un admin. */
    @PutMapping("/{id}/notifications/{notificationId}/read")
    @RequiresOwnerOrAdmin
    @ValidatesPathUser
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable String id,
            @PathVariable String notificationId) {
        notificationService.markUserNotificationAsRead(notificationId, id);
        return ResponseEntity.noContent().build();
    }
}
