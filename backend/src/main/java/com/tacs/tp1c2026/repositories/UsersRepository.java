package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.user.User;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.stream.Stream;

public interface UsersRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Stream<User> findAllBy();

  List<User> findByIdNotAndMissingCardsId(String publisherId, String cardId);

}
