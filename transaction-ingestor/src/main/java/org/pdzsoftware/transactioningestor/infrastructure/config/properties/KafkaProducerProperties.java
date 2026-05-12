package org.pdzsoftware.transactioningestor.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.kafka.producer")
public class KafkaProducerProperties {
    private String bootstrapServers;
    private String transactionsCreatedTopic;
    private String acks;
    private boolean idempotenceEnabled;
}
