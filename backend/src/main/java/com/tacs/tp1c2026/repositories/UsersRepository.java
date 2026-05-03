package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Usuario;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.mongodb.repository.Query;

public interface UsersRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Stream<Usuario> findAllBy();

  List<Usuario> findByIdNotAndMissingCardsId(String publisherId, String cardId);

}
