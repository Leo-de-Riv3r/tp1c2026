package com.tacs.tp1c2026.repositories;


import com.tacs.tp1c2026.entities.user.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Query;

public interface UserRepository extends Repository<User, String> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  // Excluye admins: el admin nunca debería ser notificado de una card disponible.
  // Devuelve el user completo (no proyección): el handler dedupea y pushea sobre su cola embebida.
  @Query(value = "{ 'missingCards.cardId' : ?0, $or: [ { 'role' : 'USER' }, { 'role' : { $exists: false } } ] }")
  List<User> findUsersSeekingCard(String cardId);
}
