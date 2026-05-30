package org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka.consumer")
public class KafkaConsumerProperties {
	private String bootstrapServers;
	private String transactionsCreatedTopic;
	private String consumerGroupId;
	private String autoOffsetReset = "earliest";
	private Retry retry = new Retry();

	@Getter
	@Setter
	public static class Retry {
		private int maxAttempts = 3;
		private long initialIntervalMs = 1000;
		private double multiplier = 2.0;
		private long maxIntervalMs = 10000;
	}
}
