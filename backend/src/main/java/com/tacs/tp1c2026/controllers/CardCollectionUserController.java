package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.input.MissingCardDto;
import com.tacs.tp1c2026.entities.dto.input.RegisterRepeatedCardDto;
import com.tacs.tp1c2026.entities.dto.output.CardDto;
import com.tacs.tp1c2026.entities.dto.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.output.RepeatedCardDto;
import com.tacs.tp1c2026.services.CollectionService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/my-collection")
public class CardCollectionUserController {
  @Autowired
  private CollectionService collectionService;

  @GetMapping("/missing")
  public ResponseEntity<PaginationDtoOutput<CardDto>> getMissingCards(
      @RequestAttribute("userId") String userId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(collectionService.getMissingCards(userId, page, size));
  }

  @PostMapping("/missing")
  public ResponseEntity<Map<String, Object>> addMissingCard(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody MissingCardDto dto
      ) {
    collectionService.addMissing(dto, userId);

    //return a json object
    Map<String, Object> body = Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Card faltante agregada con éxito"
    );
    return ResponseEntity.ok().body(body);
  }

  @GetMapping("/repeated")
  public ResponseEntity<PaginationDtoOutput<RepeatedCardDto>> getRepeatedCollection(
      @RequestAttribute("userId") String userId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer size
  ) {
    return ResponseEntity.ok(collectionService.getRepeatedCards(userId, page, size));
  }

  @PostMapping("/repeated")
  public ResponseEntity<Map<String, Object>> addRepeatedCard(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody RegisterRepeatedCardDto dto
  ) {
    collectionService.registerRepeated(dto, userId);
    //return a json object
    Map<String, Object> body = Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Figurita registrada con exito"
    );
    return ResponseEntity.ok().body(body);
  }

  @DeleteMapping("/repeated/{cardId}")
  public ResponseEntity<Map<String, Object>> deleteRepeatedCard(
      @RequestAttribute("userId") String userId,
      @PathVariable String cardId
  ) {
    collectionService.removeFromRepeated(userId, cardId);

    Map<String, Object> body = Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Figurita eliminada de coleccion con exito"
    );
    return ResponseEntity.ok().body(body);
  }

  @DeleteMapping("/missing/{cardId}")
  public ResponseEntity<Map<String, Object>> deleteMissingCard(
      @RequestAttribute("userId") String userId,
      @PathVariable String cardId
  ) {
    collectionService.removeFromMissing(userId, cardId);

    Map<String, Object> body = Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Figurita eliminada de coleccion con exito"
    );
    return ResponseEntity.ok().body(body);
  }
}
