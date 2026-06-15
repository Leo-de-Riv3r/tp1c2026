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
   * Creates a proposal on a publication. {@code id} in the body.
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TradeProposalDto>> createProposal(
      @RequestAttribute("userId") String userId,
      @Valid @RequestBody CreateTradeProposalDto body
  ) throws NotFoundException, NotFoundException, InsufficientCardException, MissingCardException {
    TradeProposal proposal = proposalService.createProposal(userId, body);
    TradeProposalDto dto = tradeMapper.mapProposal(proposal);
    return ResponseEntity
        .created(URI.create("/api/proposals/" + proposal.getId()))
        .body(ApiResponse.of("Proposal sent successfully", dto));
  }

  /**
   * Lists proposals for the current user (or {@code userId} if provided).
   * {@code role}:
   *   - "proposer" (default): proposals the user made (sent)
   *   - "publisher" / "receiver": proposals received (on the user's publications)
   * Optional filters: {@code status}, {@code id}.
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
   * Proposal detail.
   */
  @GetMapping("/{proposalId}")
  public ResponseEntity<TradeProposalDto> getProposal(
      @PathVariable String proposalId
  ) throws NotFoundException {
    TradeProposal proposal = proposalService.findProposal(proposalId);
    return ResponseEntity.ok(tradeMapper.mapProposal(proposal));
  }

  /**
   * Accepts a proposal. Triggers the full bilateral flow and creates the historical Exchange.
   * The returned {@code exchangeId} allows the FE to redirect to the Exchange detail without an extra GET.
   */
  @PutMapping("/{proposalId}/accept")
  public ResponseEntity<ApiResponse<String>> acceptProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException, MissingCardException, InsufficientCardException {
    String exchangeId = proposalService.acceptProposal(userId, proposalId);
    return ResponseEntity.ok(ApiResponse.of("Proposal accepted successfully", exchangeId));
  }

  /**
   * Rejects a proposal. Releases the proposer's compromisedCount.
   */
  @PutMapping("/{proposalId}/reject")
  public ResponseEntity<ApiResponse<Void>> rejectProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, NotFoundException, ProposalNotInPublicationException, OfferAlreadyProcessedException, ForbiddenException {
    proposalService.rejectProposal(userId, proposalId);
    return ResponseEntity.ok(ApiResponse.of("Proposal rejected"));
  }

  /**
   * Cancels a proposal from the proposer side.
   */
  @PutMapping("/{proposalId}/cancel")
  public ResponseEntity<ApiResponse<Void>> cancelProposal(
      @PathVariable String proposalId,
      @RequestAttribute("userId") String userId
  ) throws NotFoundException, NotFoundException, OfferAlreadyProcessedException, ForbiddenException {
    proposalService.cancelProposal(userId, proposalId);
    return ResponseEntity.ok(ApiResponse.of("Proposal cancelled"));
  }
}
