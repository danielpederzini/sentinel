package org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka.producer")
public class KafkaProducerProperties {
    private String bootstrapServers;
    private String transactionsScoredTopic;
    private String acks;
    private boolean idempotenceEnabled;
}
