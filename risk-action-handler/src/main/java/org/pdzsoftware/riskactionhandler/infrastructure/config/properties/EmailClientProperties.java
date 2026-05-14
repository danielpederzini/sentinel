package org.pdzsoftware.riskactionhandler.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.clients.email")
public class EmailClientProperties {
	private String destinationEmail;
}
