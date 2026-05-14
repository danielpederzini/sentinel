package org.pdzsoftware.riskactionhandler.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.clients.llm")
public class LlmRestClientProperties {
    private String baseUrl;
    private String apiKey;
    private String model;
    private Double temperature = 0.2;
    private Integer maxCompletionTokens = 512;
}
