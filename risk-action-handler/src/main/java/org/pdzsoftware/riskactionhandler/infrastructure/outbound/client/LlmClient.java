package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client;

import org.pdzsoftware.riskactionhandler.domain.exception.LlmClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.LlmRestClientProperties;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FeatureContributionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudFeaturesMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudPredictionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatCompletionRequest;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatCompletionResponse;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class LlmClient {
	private static final String ROLE_NAME = "user";
	private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
	private static final String EXPLAIN_FRAUD_PROMPT = """
			You are a fraud analyst reviewing a flagged financial transaction. \
			Based on the transaction data, fraud detection features, and model prediction below, \
			write a concise explanation (2-3 sentences) of why this transaction was flagged as suspicious.

			Focus on the most critical risk signals — such as unusual spending patterns, velocity anomalies, \
			geographic mismatches, or device/IP risk — and explain their practical implications. \
			Be direct and specific; avoid generic statements.

			""";
	private final RestClient restClient;
	private final LlmRestClientProperties properties;

	public LlmClient(@Qualifier("llmRestClient") RestClient restClient, LlmRestClientProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public String getFraudExplanation(TransactionScoredMessage transactionScoredMessage) {
		try {
			String structuredContext = buildStructuredContext(transactionScoredMessage);

			ChatMessage chatMessage = ChatMessage.builder()
					.role(ROLE_NAME)
					.content(EXPLAIN_FRAUD_PROMPT + structuredContext)
					.build();

			ChatCompletionRequest request = ChatCompletionRequest.builder()
					.model(properties.getModel())
					.messages(List.of(chatMessage))
					.temperature(properties.getTemperature())
					.maxCompletionTokens(properties.getMaxCompletionTokens())
					.stream(false)
					.build();

			ChatCompletionResponse response = restClient.post()
					.uri(CHAT_COMPLETIONS_ENDPOINT)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(ChatCompletionResponse.class);

            Objects.requireNonNull(response, "LLM returned an empty response body");

            if (response.choices() == null || response.choices().isEmpty()) {
				throw new LlmClientException("LLM returned no choices");
			}

			ChatMessage llmMessage = response.choices().getFirst().message();
			if (llmMessage == null || !StringUtils.hasText(llmMessage.content())) {
				throw new LlmClientException("LLM returned an empty completion");
			}
			return llmMessage.content();
		} catch (RestClientException | NullPointerException exception) {
			throw new LlmClientException(String.format("Failed to get LLM explainability details for transaction %s",
					transactionScoredMessage.transactionId()), exception);
		}
	}

	private String buildStructuredContext(TransactionScoredMessage msg) {
		FraudFeaturesMessage features = msg.featuresMessage();
		FraudPredictionMessage prediction = msg.predictionMessage();

		String topFeatures = prediction.explainability().topContributingFeatures().stream()
				.map(f -> "  - %s = %s (contribution: %.4f, direction: %s)".formatted(
						f.featureName(), f.featureValue(), f.contribution(), f.direction()))
				.collect(Collectors.joining("\n"));

		return """
				Transaction:
				  - Transaction ID: %s
				  - Amount: $%s
				  - Country: %s
				  - IP Address: %s
				  - Timestamp: %s

				Fraud Detection Features:
				  - User Average Amount: $%s
				  - Amount-to-Average Ratio: %.2f
				  - Transactions in Last 5 Min: %d
				  - Transactions in Last 1 Hour: %d
				  - Seconds Since Last Transaction: %d
				  - Merchant Risk Score: %.2f
				  - IP Risk Score: %.2f
				  - Device Trusted: %s
				  - Country Mismatch: %s
				  - Card Age (days): %d
				  - Hour of Day: %d

				Model Prediction:
				  - Risk Level: %s
				  - Fraud Probability: %.2f%%
				  - Model Version: %s

				Top Contributing Features:
				%s
				""".formatted(
				msg.transactionId(),
				msg.amount(),
				msg.countryCode(),
				msg.ipAddress(),
				msg.creationDateTime(),
				features.userAverageAmount(),
				features.amountToAverageRatio(),
				features.userTransactionCount5Min(),
				features.userTransactionCount1Hour(),
				features.secondsSinceLastTransaction(),
				features.merchantRiskScore(),
				features.ipRiskScore(),
				features.isDeviceTrusted(),
				features.hasCountryMismatch(),
				features.cardAgeDays(),
				features.hourOfDay(),
				prediction.riskLevel(),
				prediction.fraudProbability() * 100,
				prediction.modelVersion(),
				topFeatures
		);
	}
}
