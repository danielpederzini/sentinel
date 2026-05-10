package org.pdzsoftware.antifraudorchestrator.client;

import org.pdzsoftware.antifraudorchestrator.client.dto.FraudFeatureRequest;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudFeatureResult;
import org.pdzsoftware.antifraudorchestrator.dto.TransactionCreatedMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class FeatureManagerClient {
	private final RestClient restClient;

	public FeatureManagerClient(@Qualifier("featureManagerRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public FraudFeatureResult calculateFraudFeatures(TransactionCreatedMessage message) {
        FraudFeatureRequest request = FraudFeatureRequest.from(message);
		FraudFeatureResult response = restClient.post()
				.uri("/api/v1/fraud-features")
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(FraudFeatureResult.class);

		return Objects.requireNonNull(response, "Feature Manager returned an empty response body");
	}
}
