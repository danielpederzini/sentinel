package org.pdzsoftware.transactioningestor.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka.producer")
public class KafkaProducerProperties {
    private String bootstrapServers;
    private String transactionsCreatedTopic;
    private String acks;
    private boolean idempotenceEnabled;
}
