package org.pdzsoftware.transactioningestor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    private String bootstrapServers;
    private String transactionsCreatedTopic;
}

