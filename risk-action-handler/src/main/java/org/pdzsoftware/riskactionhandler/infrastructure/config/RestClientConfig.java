package org.pdzsoftware.riskactionhandler.infrastructure.config;

import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.LlmRestClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LlmRestClientProperties.class)
public class RestClientConfig {
	private static final String TOKEN_PREFIX = "Bearer ";

	@Bean("llmRestClient")
	public RestClient llmRestClient(RestClient.Builder builder,
									LlmRestClientProperties properties) {
		RestClient.Builder llmBuilder = builder.baseUrl(properties.getBaseUrl());
		if (StringUtils.hasText(properties.getApiKey())) {
			llmBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + properties.getApiKey());
		}
		return llmBuilder.build();
	}
}
