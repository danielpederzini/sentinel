package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client;

import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureRequest;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.domain.exception.FeatureManagerClientException;
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

	public FeatureManagerClient(@Qualifier("featureManagerRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public FraudFeatureResponse calculateFraudFeatures(TransactionCreatedMessage message) {
		try {
			FraudFeatureRequest request = FraudFeatureRequest.from(message);
			FraudFeatureResponse response = restClient.post()
					.uri(FRAUD_FEATURES_ENDPOINT)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(FraudFeatureResponse.class);

			return Objects.requireNonNull(response, "Feature Manager returned an empty response body");
		} catch (RestClientException | NullPointerException exception) {
			throw new FeatureManagerClientException("Failed to fetch fraud features from Feature Manager", exception);
		}
	}
}
