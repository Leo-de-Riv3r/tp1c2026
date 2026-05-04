package com.tacs.tp1c2026.repositories;


import com.tacs.tp1c2026.entities.profiles.ProfileGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProfileGroupRepository extends MongoRepository<ProfileGroup, Integer> {
}
