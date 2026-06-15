package com.tacs.tp1c2026.support;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersMongoConfig {

  // Si TEST_MONGODB_URI está seteada, se usa ese Mongo externo (replica set) y NO se levanta
  // Testcontainers — útil cuando el daemon Docker no es accesible desde la JVM de Maven, o para
  // reusar el Mongo del compose. OJO: usar una base dedicada (ej. tacs_test_db) porque los tests
  // dropean colecciones; NO apuntar a la base de la app. Si no está seteada, cae a Testcontainers.
  private static final String EXTERNAL_URI = System.getenv("TEST_MONGODB_URI");

  private static final MongoDBContainer mongoContainer =
      EXTERNAL_URI == null ? new MongoDBContainer("mongo:7.0") : null;

  static {
    if (mongoContainer != null) {
      mongoContainer.start();
    }
  }

  @Bean
  MongoClient testMongoClient() {
    String uri = EXTERNAL_URI != null ? EXTERNAL_URI : mongoContainer.getReplicaSetUrl("tacs_test_db");
    return MongoClients.create(uri);
  }
}
