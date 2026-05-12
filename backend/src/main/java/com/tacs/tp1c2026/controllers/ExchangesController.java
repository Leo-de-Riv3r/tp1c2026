package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.common.output.PaginationDtoOutput;
import com.tacs.tp1c2026.entities.dto.exchange.input.AddFeedbackDto;
import com.tacs.tp1c2026.entities.dto.exchange.output.ExchangeDto;
import com.tacs.tp1c2026.entities.exchange.Exchange;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.services.ExchangeService;
import com.tacs.tp1c2026.services.mappers.ExchangeMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/exchanges")
public class ExchangesController {

  private final ExchangeService exchangeService;
  private final ExchangeMapper exchangeMapper;

  public ExchangesController(ExchangeService exchangeService, ExchangeMapper exchangeMapper) {
    this.exchangeService = exchangeService;
    this.exchangeMapper = exchangeMapper;
  }

  /**
  * Lists the exchanges in which the user was involved (as A or B), paginated
  * If userId is not sent, uses the authenticated user data
  * @param userId
  * @return list of the user's exchanges
  */
  @GetMapping
  public ResponseEntity<PaginationDtoOutput<ExchangeDto>> getMyExchanges(
      @RequestAttribute("userId") String currentUserId,
      @RequestParam(required = false) String userId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer per_page
  ) throws ForbiddenException {
    // Guard: no se permite consultar exchanges de otro usuario (info leak)
    if (userId != null && !userId.equals(currentUserId)) {
      throw new ForbiddenException("No podés consultar los intercambios de otro usuario");
    }
    String targetUserId = userId != null ? userId : currentUserId;
    Page<Exchange> result = exchangeService.findByUserId(targetUserId, page, per_page);
    return ResponseEntity.ok(new PaginationDtoOutput<>(
        exchangeMapper.mapExchanges(result.getContent()),
        result.getNumber() + 1,
        result.getTotalPages()
    ));
  }

  /**
  * Returns the detail of a single exchange by its ID. Solo accesible para los participantes.
  * @param exchangeId
  * @return the exchangeDto, or 404 if not found, or 403 si el caller no participó
  */
  @GetMapping("/{exchangeId}")
  public ResponseEntity<ExchangeDto> getExchange(
      @PathVariable String exchangeId,
      @RequestAttribute("userId") String currentUserId
  ) throws NotFoundException, ForbiddenException {
    Exchange exchange = exchangeService.findById(exchangeId);
    if (!exchange.involves(currentUserId)) {
      throw new ForbiddenException("No podés consultar un intercambio del que no sos parte");
    }
    return ResponseEntity.ok(exchangeMapper.mapExchange(exchange));
  }

  /**
  * Leaves feedback about the other participant of the exchange
  * It fails if the user is not related to the exchange or if the feedback was already given
  */
  @PostMapping("/{exchangeId}/feedback")
  public ResponseEntity<Map<String, Object>> addFeedback(@PathVariable String exchangeId,
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody AddFeedbackDto body
  ) throws NotFoundException {
    exchangeService.addFeedback(exchangeId, userId, body.getScore(), body.getComment());
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Feedback registrado"
    ));
  }
}
