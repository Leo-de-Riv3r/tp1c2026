package com.tacs.tp1c2026;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.transaction.TransactionManager;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/tacs_db}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:tacs_db}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Override
    protected boolean autoIndexCreation() {
        // Indexes are created manually from the seed script (seed.js)
        // Auto-creation disabled to avoid unexpected behavior
        return false;
    }

    /**
     * Required for @Transactional to work with MongoDB
     * Has no effect unless MongoDB is running in replica set mode
     * See docker-compose.yml for replica set configuration
     */
    @Bean
    TransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}