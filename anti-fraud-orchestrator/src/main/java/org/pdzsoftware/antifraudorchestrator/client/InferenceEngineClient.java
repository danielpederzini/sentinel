package org.pdzsoftware.antifraudorchestrator.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudFeatureResult;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudPredictionRequest;
import org.pdzsoftware.antifraudorchestrator.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.exception.InferenceEngineClientException;
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
	private final ObjectMapper objectMapper;

	public InferenceEngineClient(@Qualifier("inferenceEngineRestClient") RestClient restClient,
	                            ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
	}

	public FraudPredictionResponse scoreTransaction(FraudFeatureResult features) {
		try {
			FraudPredictionRequest request = FraudPredictionRequest.from(features);
			String requestBody = objectMapper.writeValueAsString(request);
			FraudPredictionResponse response = restClient.post()
					.uri(TRANSACTION_SCORE_ENDPOINT)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.body(FraudPredictionResponse.class);

			return Objects.requireNonNull(response, "Inference Engine returned an empty response body");
		} catch (RestClientException | JsonProcessingException | NullPointerException exception) {
			throw new InferenceEngineClientException("Failed to score transaction with Inference Engine", exception);
		}
	}
}
