package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.settings.AppSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SettingsRepository extends MongoRepository<AppSettings, String> {
}
