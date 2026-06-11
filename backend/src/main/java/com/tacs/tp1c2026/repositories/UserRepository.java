package com.tacs.tp1c2026.repositories;


import com.tacs.tp1c2026.entities.user.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Query;

public interface UserRepository extends Repository<User, String> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  // Excludes admins: admin should never be notified of an available card
  @Query(value = "{ 'missingCards.cardId' : ?0, $or: [ { 'role' : 'USER' }, { 'role' : { $exists: false } } ] }", fields = "{ '_id' : 1 }")
  List<User> findUsersSeekingCard(String cardId);
}
