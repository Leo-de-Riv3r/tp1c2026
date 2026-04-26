package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Auction;
import com.tacs.tp1c2026.entities.enums.EstadoSubasta;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubastaRepository extends MongoRepository<Auction, Integer> {
  List<Auction> findByUsuarioPublicanteId(Integer userId);

  List<Auction> findByUsuarioPublicanteIdAndEstado(Integer userId, EstadoSubasta estado);

  List<Auction> findByEstado(EstadoSubasta estado);
}
