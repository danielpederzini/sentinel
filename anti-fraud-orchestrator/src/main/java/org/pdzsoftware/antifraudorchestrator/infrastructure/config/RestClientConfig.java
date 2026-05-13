package org.pdzsoftware.antifraudorchestrator.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.FeatureManagerClientProperties;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.InferenceEngineClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({FeatureManagerClientProperties.class, InferenceEngineClientProperties.class})
public class RestClientConfig {
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean("featureManagerRestClient")
	public RestClient featureManagerRestClient(RestClient.Builder builder, FeatureManagerClientProperties properties) {
		return builder.baseUrl(properties.getBaseUrl()).build();
	}

	@Bean("inferenceEngineRestClient")
	public RestClient inferenceEngineRestClient(RestClient.Builder builder, InferenceEngineClientProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(10_000);
		requestFactory.setReadTimeout(30_000);

		return builder
				.baseUrl(properties.getBaseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
