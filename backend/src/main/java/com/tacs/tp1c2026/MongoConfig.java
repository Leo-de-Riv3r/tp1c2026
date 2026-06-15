package com.tacs.tp1c2026;

import com.mongodb.client.MongoClient;
import com.tacs.tp1c2026.entities.enums.CardType;
import com.tacs.tp1c2026.entities.enums.Category;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionManager;

import java.util.List;

@Configuration
public class MongoConfig {

  @Bean
  MongoCustomConversions customConversions() {
    return new MongoCustomConversions(List.of(
        new CategoryWritingConverter(),
        new CategoryReadingConverter(),
        new CardTypeWritingConverter(),
        new CardTypeReadingConverter()
    ));
  }

  @Bean
  TransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }

  @WritingConverter
  static class CategoryWritingConverter implements Converter<Category, String> {
    @Override
    public String convert(Category source) {
      return source.getValue();
    }
  }

  @ReadingConverter
  static class CategoryReadingConverter implements Converter<String, Category> {
    @Override
    public Category convert(String source) {
      return Category.fromValue(source);
    }
  }

  @WritingConverter
  static class CardTypeWritingConverter implements Converter<CardType, String> {
    @Override
    public String convert(CardType source) {
      return source.getValue();
    }
  }

  @ReadingConverter
  static class CardTypeReadingConverter implements Converter<String, CardType> {
    @Override
    public CardType convert(String source) {
      return CardType.fromValue(source);
    }
  }

  @Component
  @ConditionalOnMissingBean(MongoClient.class)
  static class MongoUriValidator {

    @Value("${spring.mongodb.uri}")
    private String mongoUri;

    @PostConstruct
    void validate() {
      if (mongoUri == null || mongoUri.isBlank()) {
        throw new IllegalStateException(
            """
            SPRING_MONGODB_URI is not configured.

            Create a .env file at the project root with:
              SPRING_MONGODB_URI=mongodb://mongo:27017/tacs_db

            Or export it as an environment variable on your system.
            """);
      }
    }
  }
}
