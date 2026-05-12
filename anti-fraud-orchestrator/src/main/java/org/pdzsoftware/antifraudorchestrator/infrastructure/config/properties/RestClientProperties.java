package org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.clients")
public class RestClientProperties {
	private String featureManagerBaseUrl;
	private String inferenceEngineBaseUrl;
}
