package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresOwnerOrAdmin;
import com.tacs.tp1c2026.entities.dto.common.ApiResponse;
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
   * Lista los intercambios en los que participó el user (como A o B), paginados.
   * Si no se envía {@code userId}, usa el user autenticado.
   * @param userId id del user dueño (opcional)
   * @return lista de intercambios del user
   */
  @GetMapping
  @RequiresOwnerOrAdmin(value = "userId", source = RequiresOwnerOrAdmin.Source.QUERY)
  public ResponseEntity<PaginationDtoOutput<ExchangeDto>> getMyExchanges(
      @RequestAttribute("userId") String currentUserId,
      @RequestParam(required = false) String userId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10") Integer per_page
  ) {
    String targetUserId = userId != null ? userId : currentUserId;
    Page<Exchange> result = exchangeService.findByUserId(targetUserId, page, per_page);
    return ResponseEntity.ok(new PaginationDtoOutput<>(
        exchangeMapper.mapExchanges(result.getContent()),
        result.getNumber() + 1,
        result.getTotalPages()
    ));
  }

  /**
   * Devuelve el detalle de un intercambio por su ID. Solo accesible para los participantes.
   * @param exchangeId id del intercambio
   * @return el {@link ExchangeDto}; 404 si no existe, 403 si el caller no participó
   */
  @GetMapping("/{exchangeId}")
  public ResponseEntity<ExchangeDto> getExchange(
      @PathVariable String exchangeId,
      @RequestAttribute("userId") String currentUserId
  ) throws NotFoundException, ForbiddenException {
    Exchange exchange = exchangeService.findById(exchangeId);
    if (!exchange.involves(currentUserId)) {
      throw new ForbiddenException("No podés ver un intercambio en el que no participaste");
    }
    return ResponseEntity.ok(exchangeMapper.mapExchange(exchange));
  }

  /**
   * Deja una calificación sobre la otra parte del intercambio.
   * Falla si el user no participa del intercambio o si ya dejó su calificación.
   */
  @PostMapping("/{exchangeId}/feedback")
  public ResponseEntity<ApiResponse<Void>> addFeedback(@PathVariable String exchangeId,
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody AddFeedbackDto body
  ) throws NotFoundException {
    exchangeService.addFeedback(exchangeId, userId, body.getScore(), body.getComment());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("Calificación registrada"));
  }
}
