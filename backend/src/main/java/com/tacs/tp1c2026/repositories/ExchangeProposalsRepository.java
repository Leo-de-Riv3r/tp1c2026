package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.ExchangeProposal;
import com.tacs.tp1c2026.entities.enums.ProposalState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExchangeProposalsRepository extends MongoRepository<ExchangeProposal, String> {
  Page<ExchangeProposal> findByReceiverIdAndState(String receiverId, ProposalState state, Pageable pageable);
  Page<ExchangeProposal> findByExchangeUserIdAndState(String userId, ProposalState state, Pageable pageable);
  Page<ExchangeProposal> findByPublicationId(String publicationId, Pageable pageable);
}
