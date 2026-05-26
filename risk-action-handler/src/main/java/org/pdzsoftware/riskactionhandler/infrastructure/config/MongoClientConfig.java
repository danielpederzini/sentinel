package org.pdzsoftware.riskactionhandler.infrastructure.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Slf4j
@Configuration
public class MongoClientConfig {
    private static final String DEFAULT_URI = "mongodb://localhost:27017/risk-action-handler";

    @Bean
    public MongoClient mongoClient() {
        String uri = resolveMongoUri();
        log.info("Connecting to MongoDB at: {}", uri);
        return MongoClients.create(uri);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        ConnectionString connectionString = new ConnectionString(resolveMongoUri());
        String database = connectionString.getDatabase();
        if (database == null || database.isBlank()) {
            database = "risk-action-handler";
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    private String resolveMongoUri() {
        String uri = System.getenv("MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            uri = System.getenv("SPRING_DATA_MONGODB_URI");
        }
        return (uri != null && !uri.isBlank()) ? uri : DEFAULT_URI;
    }
}
