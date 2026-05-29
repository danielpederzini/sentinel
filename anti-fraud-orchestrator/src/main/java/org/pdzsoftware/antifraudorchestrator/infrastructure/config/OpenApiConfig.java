package org.pdzsoftware.antifraudorchestrator.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI antiFraudOrchestratorOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Anti-Fraud Orchestrator API")
				.description("""
						Orchestrates fraud scoring across the Sentinel platform. Exposes the JWKS endpoint \
						used by downstream services to verify service-to-service JWTs.""")
				.version("0.0.1"));
	}
}
