package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client;

import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionRequest;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.domain.exception.InferenceEngineClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

@Component
public class InferenceEngineClient {
	private static final String TRANSACTION_SCORE_ENDPOINT = "/transaction/score";
	private final RestClient restClient;

	public InferenceEngineClient(@Qualifier("inferenceEngineRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public FraudPredictionResponse scoreTransaction(FraudFeatureResponse features) {
		try {
			FraudPredictionRequest request = FraudPredictionRequest.from(features);
			FraudPredictionResponse response = restClient.post()
					.uri(TRANSACTION_SCORE_ENDPOINT)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(FraudPredictionResponse.class);

			return Objects.requireNonNull(response, "Inference Engine returned an empty response body");
		} catch (RestClientException | NullPointerException exception) {
			throw new InferenceEngineClientException("Failed to score transaction with Inference Engine", exception);
		}
	}
}
