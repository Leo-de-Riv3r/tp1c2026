package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradeProposalDTO;
import com.tacs.tp1c2026.entities.dto.trade.output.TradeProposalDto;
import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.services.ProposalService;
import com.tacs.tp1c2026.services.mappers.TradeMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proposals")
public class ProposalsController {

  private final ProposalService proposalService;
  private final TradeMapper tradeMapper;

  public ProposalsController(ProposalService proposalService, TradeMapper tradeMapper) {
    this.proposalService = proposalService;
    this.tradeMapper = tradeMapper;
  }

  /**
   * Crea una propuesta sobre una publicación. {@code publicationId} en el body.
   */
  @PostMapping
  public ResponseEntity<Map<String, Object>> createProposal(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateTradeProposalDTO body
  ) throws UserNotFoundException, NotFoundException, InsufficientCardException, MissingCardException, NoAvailableSlotsException {
    TradeProposal proposal = proposalService.createProposal(userId, body);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Propuesta enviada con éxito",
        "proposalId", proposal.getId()
    ));
  }

  /**
   * Lista propuestas del usuario actual (o de {@code userId} si se pasa).
   * {@code role}:
   *   - "proposer" (default): propuestas que el usuario hizo (enviadas)
   *   - "publisher" / "receiver": propuestas recibidas (sobre publicaciones del usuario)
   * Filtros opcionales: {@code status}, {@code publicationId}.
   */
  @GetMapping
  public ResponseEntity<List<TradeProposalDto>> listProposals(
      @RequestAttribute("userId") String currentUserId,
      @RequestParam(required = false) String userId,
      @RequestParam(required = false, defaultValue = "proposer") String role,
      @RequestParam(required = false) TradeProposalStatus status,
      @RequestParam(required = false) String publicationId
  ) {
    List<TradeProposal> result;
    if (publicationId != null) {
      result = proposalService.findByPublicationId(publicationId);
      if (status != null) {
        result = result.stream().filter(p -> p.getStatus() == status).toList();
      }
    } else {
      String targetUserId = userId != null ? userId : currentUserId;
      result = proposalService.searchProposals(targetUserId, role, status);
    }
    return ResponseEntity.ok(tradeMapper.mapProposals(result));
  }

  /**
   * Detalle de una propuesta.
   */
  @GetMapping("/{proposalId}")
  public ResponseEntity<TradeProposalDto> getProposal(
      @PathVariable String proposalId
  ) throws NotFoundException {
    TradeProposal proposal = proposalService.findProposal(proposalId);
    return ResponseEntity.ok(tradeMapper.mapProposal(proposal));
  }

  /**
   * Aceptar una propuesta. Dispara el flujo bilateral completo y crea el Exchange histórico.
   */
  @PutMapping("/{proposalId}/accept")
  public ResponseEntity<Map<String, Object>> acceptProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws UserNotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException, MissingCardException, InsufficientCardException {
    proposalService.acceptProposal(userId, proposalId);
    return ResponseEntity.ok(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Propuesta aceptada con éxito"
    ));
  }

  /**
   * Rechazar una propuesta. Libera el compromisedCount del proponente.
   */
  @PutMapping("/{proposalId}/reject")
  public ResponseEntity<Map<String, Object>> rejectProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws UserNotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException {
    proposalService.rejectProposal(userId, proposalId);
    return ResponseEntity.ok(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Propuesta rechazada"
    ));
  }

  /**
   * Cancelar una propuesta del lado del proponente.
   */
  @PutMapping("/{proposalId}/cancel")
  public ResponseEntity<Map<String, Object>> cancelProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws UserNotFoundException, NotFoundException, OfferAlreadyProcessedException, ForbiddenException {
    proposalService.cancelProposal(userId, proposalId);
    return ResponseEntity.ok(Map.of(
        "timestamp", LocalDateTime.now(),
        "message", "Propuesta cancelada"
    ));
  }
}
