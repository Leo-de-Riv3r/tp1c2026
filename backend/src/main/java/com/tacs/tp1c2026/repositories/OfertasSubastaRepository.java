package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.AuctionOffer;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OfertasSubastaRepository extends MongoRepository<AuctionOffer, Integer> {
  List<AuctionOffer> findByAuctionId(Integer subastaId);

  List<AuctionOffer> findByUsuarioPostorId(Integer userId);

  List<AuctionOffer> findByAuctionUsuarioPublicanteId(Integer userId);
}
