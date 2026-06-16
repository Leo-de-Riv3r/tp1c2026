package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.config.RequiresOwnerOrAdmin;
import com.tacs.tp1c2026.entities.dto.common.ApiResponse;
import com.tacs.tp1c2026.entities.dto.trade.input.CreateTradeProposalDto;
import com.tacs.tp1c2026.entities.dto.trade.output.TradeProposalDto;
import com.tacs.tp1c2026.entities.enums.TradeProposalStatus;
import com.tacs.tp1c2026.entities.exchange.TradeProposal;
import com.tacs.tp1c2026.exceptions.*;
import com.tacs.tp1c2026.services.ProposalService;
import com.tacs.tp1c2026.services.mappers.TradeMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
   * Crea una propuesta sobre una publicación. El {@code id} de la publicación viaja en el body.
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TradeProposalDto>> createProposal(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateTradeProposalDto body
  ) throws NotFoundException, NotFoundException, ConflictException, NotFoundException {
    TradeProposal proposal = proposalService.createProposal(userId, body);
    TradeProposalDto dto = tradeMapper.mapProposal(proposal);
    return ResponseEntity
        .created(URI.create("/api/proposals/" + proposal.getId()))
        .body(ApiResponse.of("Propuesta enviada correctamente", dto));
  }

  /**
   * Lista las propuestas del user actual (o de {@code userId} si se envía).
   * {@code role}:
   *   - "proposer" (default): propuestas que el user hizo (enviadas).
   *   - "publisher" / "receiver": propuestas recibidas (sobre sus publicaciones).
   * Filtros opcionales: {@code status}, {@code publicationId}.
   */
  @GetMapping
  @RequiresOwnerOrAdmin(value = "userId", source = RequiresOwnerOrAdmin.Source.QUERY)
  public ResponseEntity<List<TradeProposalDto>> listProposals(
      @RequestAttribute("userId") String currentUserId,
      @RequestParam(required = false) String userId,
      @RequestParam(required = false, defaultValue = "proposer") String role,
      @RequestParam(required = false) TradeProposalStatus status,
      @RequestParam(required = false) String publicationId
  ) {
    List<TradeProposal> result;
    if (publicationId != null) {
      result = proposalService.findByPublicationIdForUser(publicationId, currentUserId);
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
   * Detalle de propuesta.
   */
  @GetMapping("/{proposalId}")
  public ResponseEntity<TradeProposalDto> getProposal(
      @PathVariable String proposalId
  ) throws NotFoundException {
    TradeProposal proposal = proposalService.findProposal(proposalId);
    return ResponseEntity.ok(tradeMapper.mapProposal(proposal));
  }

  /**
   * Acepta una propuesta. Dispara el flow bilateral completo y crea el Exchange histórico.
   * El {@code exchangeId} devuelto le permite al FE redirigir al detalle del intercambio sin un GET extra.
   */
  @PutMapping("/{proposalId}/accept")
  public ResponseEntity<ApiResponse<String>> acceptProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, NotFoundException, BadInputException, ConflictException, ForbiddenException, NotFoundException, ConflictException {
    String exchangeId = proposalService.acceptProposal(userId, proposalId);
    return ResponseEntity.ok(ApiResponse.of("Propuesta aceptada correctamente", exchangeId));
  }

  /**
   * Rechaza una propuesta. Libera el {@code compromisedCount} del proponente.
   */
  @PutMapping("/{proposalId}/reject")
  public ResponseEntity<ApiResponse<Void>> rejectProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, NotFoundException, BadInputException, ConflictException, ForbiddenException {
    proposalService.rejectProposal(userId, proposalId);
    return ResponseEntity.ok(ApiResponse.of("Propuesta rechazada"));
  }

  /**
   * Cancela una propuesta desde el lado del proponente.
   */
  @PutMapping("/{proposalId}/cancel")
  public ResponseEntity<ApiResponse<Void>> cancelProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, NotFoundException, ConflictException, ForbiddenException {
    proposalService.cancelProposal(userId, proposalId);
    return ResponseEntity.ok(ApiResponse.of("Propuesta cancelada"));
  }
}
