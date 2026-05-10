package org.pdzsoftware.antifraudorchestrator.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudFeatureRequest;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudFeatureResult;
import org.pdzsoftware.antifraudorchestrator.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.exception.FeatureManagerClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

@Component
public class FeatureManagerClient {
	private static final String FRAUD_FEATURES_ENDPOINT = "/api/v1/fraud-features";
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public FeatureManagerClient(@Qualifier("featureManagerRestClient") RestClient restClient,
	                           ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
	}

	public FraudFeatureResult calculateFraudFeatures(TransactionCreatedMessage message) {
		try {
			FraudFeatureRequest request = FraudFeatureRequest.from(message);
			String requestBody = objectMapper.writeValueAsString(request);
			FraudFeatureResult response = restClient.post()
					.uri(FRAUD_FEATURES_ENDPOINT)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.body(FraudFeatureResult.class);

			return Objects.requireNonNull(response, "Feature Manager returned an empty response body");
		} catch (RestClientException | JsonProcessingException | NullPointerException exception) {
			throw new FeatureManagerClientException("Failed to fetch fraud features from Feature Manager", exception);
		}
	}
}
