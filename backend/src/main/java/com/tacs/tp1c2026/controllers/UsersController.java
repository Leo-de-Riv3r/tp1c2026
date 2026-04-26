package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.user.input.*;
import com.tacs.tp1c2026.entities.dto.user.output.CollectionCardResult;
import com.tacs.tp1c2026.entities.dto.user.output.UserDto;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns all registered users as DTOs. Intended for the admin view
     * @return list of users as {@link UserDto}
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.getAll().stream()
                .map(UserDto::from)
                .toList());
    }

    /**
     * Returns a user by their MongoDB ID
     * @param id the user's ID
     * @return the user as {@link UserDto}, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(UserDto.from(userService.getById(id)));
    }

    /* Collection endpoints */

    /**
     * Returns the card collection of a user
     * @param id the user's ID
     * @return list of cards in the user's collection
     */
    @GetMapping("/{id}/collection")
    public ResponseEntity<List<CollectionCard>> getCollection(@PathVariable String id) {
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
    public ResponseEntity<CollectionCard> addToCollection(
            @PathVariable String id,
            @Valid @RequestBody AddToCollectionRequest request) {
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
    public ResponseEntity<Void> decrementFromCollection(
            @PathVariable String id,
            @PathVariable String cardId) {
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
    public ResponseEntity<List<MissingCard>> getMissingCards(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserMissingCards(id));
    }

    /**
     * Marks a card as missing for the user. Does nothing if it's already on the list.
     * @param id the user's ID
     * @param request body containing the card ID to mark as missing
     * @return 201 with the created {@link MissingCard} entry
     */
    @PostMapping("/{id}/missing-cards")
    public ResponseEntity<MissingCard> addMissingCard(
            @PathVariable String id,
            @Valid @RequestBody AddMissingCardRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(userService.addMissingCard(id, request.cardId()));
    }
}