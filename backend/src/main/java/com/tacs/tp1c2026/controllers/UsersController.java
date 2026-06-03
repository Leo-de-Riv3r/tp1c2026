package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.user.input.*;
import com.tacs.tp1c2026.entities.dto.user.output.CollectionCardResult;
import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.user.output.NotificationDto;
import com.tacs.tp1c2026.entities.dto.user.output.SuggestionResult;
import com.tacs.tp1c2026.entities.dto.user.output.UserDto;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.config.RequiresOwnerOrAdmin;
import com.tacs.tp1c2026.config.RequiresRole;
import com.tacs.tp1c2026.entities.enums.NotificationStatus;
import com.tacs.tp1c2026.entities.notification.Notification;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import com.tacs.tp1c2026.exceptions.MissingCardException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
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
     * Returns all registered users as DTOs. Intended for the admin view
     * @return list of users as {@link UserDto}
     */
    @GetMapping
    @RequiresRole("ADMIN")
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.getAll().stream()
                .map(UserDto::from)
                .toList());
    }

    /**
     * Returns a user by their MongoDB ID. Solo accesible para el propio user o ADMIN
     * @param id the user's ID
     * @return the user as {@link UserDto}, or 404 if not found
     */
    @GetMapping("/{id}")
    @RequiresOwnerOrAdmin
    public ResponseEntity<UserDto> getById(@PathVariable String id) throws UserNotFoundException {
        return ResponseEntity.ok(UserDto.from(userService.getById(id)));
    }

    /* Collection endpoints */

    /**
     * Returns the card collection of a user
     * @param id the user's ID
     * @return list of cards in the user's collection
     */
    @GetMapping("/{id}/collection")
    @RequiresOwnerOrAdmin
    public ResponseEntity<List<CollectionCard>> getCollection(@PathVariable String id) throws UserNotFoundException {
        return ResponseEntity.ok(userService.getUserCardCollection(id));
    }

    /**
     * Adds a card to the user's collection. If the card is already there, increments its quantity.
     * Returns 201 if the card was added for the first time, 200 if the quantity was incremented.
     * @param id the user's ID
     * @param request body containing the card ID to add
     * @return the updated {@link CollectionCard} entry
     */
    @PostMapping("/{id}/collection")
    @RequiresOwnerOrAdmin
    public ResponseEntity<CollectionCard> addToCollection(
            @PathVariable String id,
            @Valid @RequestBody AddToCollectionRequest request) throws MissingCardException, NotFoundException, UserNotFoundException {
        CollectionCardResult result = userService.addCardToUserCollection(id, request.cardId());
        return ResponseEntity
            .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
            .body(result.card());
    }

    /**
     * Decrements the quantity of a card in the user's collection by one.
     * Removes the entry entirely if the quantity reaches zero.
     * @param id the user's ID
     * @param cardId the card ID to decrement
     */
    @PatchMapping("/{id}/collection/{cardId}")
    @RequiresOwnerOrAdmin
    public ResponseEntity<Void> decrementFromCollection(
            @PathVariable String id,
            @PathVariable String cardId) throws InsufficientCardException, MissingCardException, NotFoundException, UserNotFoundException {
        userService.decrementFromCollection(id, cardId);
        return ResponseEntity.noContent().build();
    }

    /* Missing cards endpoints */

    /**
     * Returns the list of cards the user is looking for
     * @param id the user's ID
     * @return list of missing cards
     */
    @GetMapping("/{id}/missing-cards")
    @RequiresOwnerOrAdmin
    public ResponseEntity<List<MissingCard>> getMissingCards(@PathVariable String id) throws UserNotFoundException {
        return ResponseEntity.ok(userService.getUserMissingCards(id));
    }

    /**
     * Marks a card as missing for the user. Does nothing if it's already on the list.
     * @param id the user's ID
     * @param request body containing the card ID to mark as missing
     * @return 201 with the created {@link MissingCard} entry
     */
    @PostMapping("/{id}/missing-cards")
    @RequiresOwnerOrAdmin
    public ResponseEntity<MissingCard> addMissingCard(
            @PathVariable String id,
            @Valid @RequestBody AddMissingCardRequest request) throws NotFoundException, UserNotFoundException {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(userService.addMissingCard(id, request.cardId()));
    }

    /**
     * Removes a card from the user's missing list.
     * @param id the user's ID
     * @param cardId the card ID to remove
     */
    @DeleteMapping("/{id}/missing-cards/{cardId}")
    @RequiresOwnerOrAdmin
    public ResponseEntity<Void> removeMissingCard(
            @PathVariable String id,
            @PathVariable String cardId) throws NotFoundException, UserNotFoundException {
        userService.removeFromMissingCards(id, cardId);
        return ResponseEntity.noContent().build();
    }

    /* Suggestions endpoint */

    /**
     * Returns the exchange suggestions for a user.
     * Suggestions are computed periodically by the scheduled SuggestionGenerator.
     * Each suggestion points to a specific active publication or auction (from another user
     * with similar profile) whose card matches one of this user's missing cards.
     * @param id the user's ID
     * @return list of {@link SuggestionResult}
     */
    @GetMapping("/{id}/suggestions")
    @RequiresOwnerOrAdmin
    public ResponseEntity<List<SuggestionResult>> getSuggestions(@PathVariable String id) throws UserNotFoundException {
        return ResponseEntity.ok(
            userService.getUserSuggestions(id).stream()
                .map(SuggestionResult::from)
                .toList()
        );
    }

    /* Notification endpoints */

    @GetMapping("/{id}/notifications")
    @RequiresOwnerOrAdmin
    public ResponseEntity<PaginationDtoOutput<NotificationDto>> getNotifications(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer per_page,
            @RequestParam(required = true) NotificationStatus status) {
        Page<Notification> result = notificationService.getNotificationsForUser(id, status, page, per_page);
        List<NotificationDto> dtos = result.getContent().stream().map(NotificationDto::from).toList();
        return ResponseEntity.ok(new PaginationDtoOutput<>(dtos, result.getNumber() + 1, result.getTotalPages()));
    }

    @PutMapping("/{id}/notifications/read")
    @RequiresOwnerOrAdmin
    public ResponseEntity<Void> markAllNotificationsAsRead(@PathVariable String id) {
        notificationService.markAllUserNotificationsAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/notifications/{notificationId}/read")
    @RequiresOwnerOrAdmin
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable String id,
            @PathVariable String notificationId) {
        notificationService.markUserNotificationAsRead(notificationId, id);
        return ResponseEntity.noContent().build();
    }
}
