package org.pdzsoftware.antifraudorchestrator.client;

import org.pdzsoftware.antifraudorchestrator.client.dto.FraudFeatureResult;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudPredictionRequest;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudPredictionResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class InferenceEngineClient {
	private final RestClient restClient;

	public InferenceEngineClient(@Qualifier("inferenceEngineRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public FraudPredictionResponse scoreTransaction(FraudFeatureResult features) {
		FraudPredictionRequest request = FraudPredictionRequest.from(features);
		FraudPredictionResponse response = restClient.post()
				.uri("/transaction/score")
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(FraudPredictionResponse.class);

		return Objects.requireNonNull(response, "Inference Engine returned an empty response body");
	}
}
