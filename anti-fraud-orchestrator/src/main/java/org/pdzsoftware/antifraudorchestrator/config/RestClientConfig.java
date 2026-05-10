package org.pdzsoftware.antifraudorchestrator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientConfig {
	@Bean("featureManagerRestClient")
	public RestClient featureManagerRestClient(RestClient.Builder builder, RestClientProperties properties) {
		return builder.baseUrl(properties.getFeatureManagerBaseUrl()).build();
	}

	@Bean("inferenceEngineRestClient")
	public RestClient inferenceEngineRestClient(RestClient.Builder builder, RestClientProperties properties) {
		return builder.baseUrl(properties.getInferenceEngineBaseUrl()).build();
	}
}
