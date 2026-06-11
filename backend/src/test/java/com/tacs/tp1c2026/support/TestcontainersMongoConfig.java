package com.tacs.tp1c2026.support;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersMongoConfig {

  private static final MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0");

  static {
    mongoContainer.start();
  }

  @Bean
  MongoClient testMongoClient() {
    return MongoClients.create(mongoContainer.getReplicaSetUrl("tacs_test_db"));
  }
}
