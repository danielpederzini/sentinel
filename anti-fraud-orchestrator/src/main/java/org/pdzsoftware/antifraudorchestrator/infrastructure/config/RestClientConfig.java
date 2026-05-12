package org.pdzsoftware.antifraudorchestrator.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.RestClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientConfig {
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean("featureManagerRestClient")
	public RestClient featureManagerRestClient(RestClient.Builder builder, RestClientProperties properties) {
		return builder.baseUrl(properties.getFeatureManagerBaseUrl()).build();
	}

	@Bean("inferenceEngineRestClient")
	public RestClient inferenceEngineRestClient(RestClient.Builder builder, RestClientProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(10_000);
		requestFactory.setReadTimeout(30_000);

		return builder
				.baseUrl(properties.getInferenceEngineBaseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
