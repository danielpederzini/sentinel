package org.pdzsoftware.antifraudorchestrator.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.FeatureManagerClientProperties;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.InferenceEngineClientProperties;
import org.pdzsoftware.antifraudorchestrator.infrastructure.config.properties.ServiceAuthProperties;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.ServiceTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
		FeatureManagerClientProperties.class,
		InferenceEngineClientProperties.class,
		ServiceAuthProperties.class
})
public class RestClientConfig {
	private static final int INFERENCE_CONNECT_TIMEOUT_MS = 10_000;
	private static final int INFERENCE_READ_TIMEOUT_MS = 30_000;

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public ClientHttpRequestInterceptor serviceAuthInterceptor(ServiceTokenProvider tokenProvider) {
		return (request, body, execution) -> {
			request.getHeaders().setBearerAuth(tokenProvider.getToken());
			return execution.execute(request, body);
		};
	}

	@Bean("featureManagerRestClient")
	public RestClient featureManagerRestClient(RestClient.Builder builder, FeatureManagerClientProperties properties,
			ClientHttpRequestInterceptor serviceAuthInterceptor) {
		return builder
				.baseUrl(properties.getBaseUrl())
				.requestInterceptor(serviceAuthInterceptor)
				.build();
	}

	@Bean("inferenceEngineRestClient")
	public RestClient inferenceEngineRestClient(RestClient.Builder builder, InferenceEngineClientProperties properties,
			ClientHttpRequestInterceptor serviceAuthInterceptor) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(INFERENCE_CONNECT_TIMEOUT_MS);
		requestFactory.setReadTimeout(INFERENCE_READ_TIMEOUT_MS);

		return builder
				.baseUrl(properties.getBaseUrl())
				.requestFactory(requestFactory)
				.requestInterceptor(serviceAuthInterceptor)
				.build();
	}
}
