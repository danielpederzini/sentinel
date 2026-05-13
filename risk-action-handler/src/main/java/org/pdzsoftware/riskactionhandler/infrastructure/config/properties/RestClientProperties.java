package org.pdzsoftware.riskactionhandler.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.clients")
public class RestClientProperties {
    private String llmBaseUrl;
    private String llmApiKey;
    private String llmModel;
    private Double llmTemperature = 0.2;
    private Integer llmMaxCompletionTokens = 512;
}
