package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.session.Session;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SessionRepository extends MongoRepository<Session, String> {
}
