package org.pdzsoftware.transactioningestor.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI transactionIngestorOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Transaction Ingestor API")
				.description("""
						Public entrypoint for submitting transactions into the Sentinel fraud detection platform. \
						Accepted transactions are published to Kafka for asynchronous fraud scoring.""")
				.version("0.0.1"));
	}
}
