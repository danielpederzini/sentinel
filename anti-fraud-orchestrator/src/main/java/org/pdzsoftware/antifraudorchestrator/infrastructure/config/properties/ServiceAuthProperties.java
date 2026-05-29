package org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.service-auth")
public class ServiceAuthProperties {
	private String issuer;
	private String audience;
	private long tokenTtlSeconds = 60;
}
