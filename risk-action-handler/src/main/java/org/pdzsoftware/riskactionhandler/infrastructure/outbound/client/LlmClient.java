package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client;

import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.domain.exception.LlmClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.LlmRestClientProperties;

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
import java.util.stream.Collectors;

@Slf4j
@Component
public class LlmClient {
	private static final String SYSTEM_ROLE = "system";
	private static final String USER_ROLE = "user";
	private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";

	private static final String SYSTEM_PROMPT = """
			You are a senior fraud analyst at a financial institution. You are reviewing a transaction \
			that was flagged by an ML-based fraud detection pipeline. Your job is to write a concise, \
			actionable explanation (2-3 sentences) of why this transaction was flagged.

			Rules:
			- Reference concrete values from the data (e.g., amounts, ratios, scores, time gaps).
			- Explain what makes the values abnormal (e.g., "3.5x the user's average" rather than "high amount").
			- Prioritize the top SHAP contributors — these are the features the model weighted most heavily.
			- Do NOT repeat raw feature names; translate them into plain language.
			- Do NOT hedge or use filler phrases like "may be" or "could indicate".

			Feature glossary (so you understand what each metric measures):
			- logAmount: Natural log of the transaction amount in dollars.
			- logSecondsSinceLastTransaction: Natural log of seconds elapsed since the user's previous transaction.
			- logVelocity1Hour: Natural log of the user's total spend in the last hour.
			- amountTimesMerchantRisk: Transaction amount multiplied by the merchant's historical fraud rate.
			- riskScoreProduct: Product of merchant risk score and IP risk score.
			- ipDeviceRisk: IP risk score adjusted by device trust (0 if device is trusted, IP risk if untrusted).
			- countryIpRisk: IP risk score multiplied by country-mismatch flag (0 if no mismatch).
			- velocityAmountInteraction: Product of hourly transaction count and amount-to-average ratio.
			- recencyVelocity: Inverse of seconds since last transaction multiplied by hourly transaction count.
			- amountDeviation: How many standard deviations this amount is from the user's average.
			- isNight: Whether the transaction occurred between 23:00-05:00 local time.
			- velocityIntensity: Hourly transaction count multiplied by hourly spend volume.
			- amountToAverageRatio: Transaction amount divided by the user's historical average amount.
			- merchantRiskScore: Merchant's historical fraud rate (0.0 = clean, 1.0 = all fraudulent).
			- ipRiskScore: IP address historical fraud rate (0.0 = clean, 1.0 = all fraudulent).
			""";

	private static final String USER_PROMPT_TEMPLATE = """
			Analyze the following flagged transaction and provide your explanation.

			%s""";

	private final RestClient restClient;
	private final LlmRestClientProperties properties;

	public LlmClient(@Qualifier("llmRestClient") RestClient restClient, LlmRestClientProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public String getFraudExplanation(TransactionScoredMessage transactionScoredMessage) {
		try {
			String structuredContext = buildStructuredContext(transactionScoredMessage);

			ChatMessage systemMessage = ChatMessage.builder()
					.role(SYSTEM_ROLE)
					.content(SYSTEM_PROMPT)
					.build();

			ChatMessage userMessage = ChatMessage.builder()
					.role(USER_ROLE)
					.content(USER_PROMPT_TEMPLATE.formatted(structuredContext))
					.build();

			ChatCompletionRequest request = ChatCompletionRequest.builder()
					.model(properties.getModel())
					.messages(List.of(systemMessage, userMessage))
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

            if (response == null) {
				throw new LlmClientException("LLM returned an empty response body");
			}

            if (response.choices() == null || response.choices().isEmpty()) {
				throw new LlmClientException("LLM returned no choices");
			}

			ChatMessage llmMessage = response.choices().getFirst().message();
			if (llmMessage == null || !StringUtils.hasText(llmMessage.content())) {
				throw new LlmClientException("LLM returned an empty completion");
			}
			return llmMessage.content();
		} catch (RestClientException exception) {
			log.error("LLM explanation request failed for transaction {}", transactionScoredMessage.transactionId(), exception);
			throw new LlmClientException(String.format("Failed to get LLM explainability details for transaction %s",
					transactionScoredMessage.transactionId()), exception);
		}
	}

	private String buildStructuredContext(TransactionScoredMessage message) {
		FraudPredictionMessage prediction = message.predictionMessage();
		FraudFeaturesMessage features = message.featuresMessage();

		String topFeatures = prediction.topContributingFeatures().stream()
				.map(feature -> "  - %s = %s (SHAP contribution: %.4f, direction: %s)".formatted(
						feature.featureName(), feature.featureValue(), feature.contribution(), feature.direction()))
				.collect(Collectors.joining("\n"));

		return """
				Transaction:
				  - Transaction ID: %s
				  - Amount: $%s
				  - Country: %s
				  - IP Address: %s
				  - Device ID: %s
				  - Merchant ID: %s
				  - Timestamp: %s

				Model Prediction:
				  - Fraud Probability: %.2f%%
				  - Risk Level: %s

				Behavioral Context:
				  - User Average Amount: $%s
				  - Amount-to-Average Ratio: %.2f
				  - Transactions in Last 5 Minutes: %d
				  - Transactions in Last Hour: %d
				  - Seconds Since Last Transaction: %d
				  - Amount Velocity (Last Hour): $%s
				  - Distinct Merchants (Last Hour): %d
				  - Card Age (Days): %d
				  - Account Age (Days): %d
				  - Hour of Day: %d

				Risk Indicators:
				  - Merchant Risk Score: %.2f
				  - IP Risk Score: %.2f
				  - Device Trusted: %s
				  - Country Mismatch: %s
				  - Is Night Transaction: %s

				Top SHAP Contributing Features (ranked by model impact):
				%s
				""".formatted(
				message.transactionId(),
				message.amount(),
				message.countryCode(),
				message.ipAddress(),
				message.deviceId(),
				message.merchantId(),
				message.creationDateTime(),
				prediction.fraudProbability() * 100,
				prediction.riskLevel(),
				features.userAverageAmount(),
				features.amountToAverageRatio(),
				features.userTransactionCount5Min(),
				features.userTransactionCount1Hour(),
				features.secondsSinceLastTransaction(),
				features.amountVelocity1Hour(),
				features.distinctMerchantCount1Hour(),
				features.cardAgeDays(),
				features.userAccountAgeDays(),
				features.hourOfDay(),
				features.merchantRiskScore(),
				features.ipRiskScore(),
				features.isDeviceTrusted(),
				features.hasCountryMismatch(),
				features.isNight(),
				topFeatures
		);
	}
}
