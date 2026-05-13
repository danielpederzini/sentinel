package org.pdzsoftware.riskactionhandler.infrastructure.config;

import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.RestClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientConfig {
	@Bean("llmRestClient")
	public RestClient llmRestClient(RestClient.Builder builder, RestClientProperties properties) {
		RestClient.Builder llmBuilder = builder.baseUrl(properties.getLlmBaseUrl());
		if (StringUtils.hasText(properties.getLlmApiKey())) {
			llmBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getLlmApiKey());
		}
		return llmBuilder.build();
	}
}
