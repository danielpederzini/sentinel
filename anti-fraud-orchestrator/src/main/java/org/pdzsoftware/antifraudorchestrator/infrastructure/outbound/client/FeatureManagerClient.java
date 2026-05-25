package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client;

import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureRequest;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.PersistTransactionRequest;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.domain.exception.FeatureManagerClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Objects;

@Slf4j
@Component
public class FeatureManagerClient {
	private static final String FRAUD_FEATURES_ENDPOINT = "/api/v1/fraud-features";
	private static final String PERSIST_TRANSACTION_ENDPOINT = "/api/v1/transactions";
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
			log.error("Failed to fetch fraud features for transaction {}", message.transactionId(), exception);
			throw new FeatureManagerClientException("Failed to fetch fraud features from Feature Manager", exception);
		}
	}

	public void persistTransaction(PersistTransactionRequest request) {
		try {
			restClient.post()
					.uri(PERSIST_TRANSACTION_ENDPOINT)
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException exception) {
			log.error("Failed to persist transaction {} via Feature Manager", request.transactionId(), exception);
			throw new FeatureManagerClientException(String.format(
					"Failed to persist transaction %s via Feature Manager", request.transactionId()), exception);
		}
	}
}
