package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client;

import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FeatureContributionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudFeaturesMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudPredictionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;

import java.util.List;
import java.util.stream.Collectors;

public final class EmailContentBuilder {

	private EmailContentBuilder() {
	}

	public static String buildFraudAlertEmail(TransactionScoredMessage msg, String llmExplanation) {
		FraudFeaturesMessage features = msg.featuresMessage();
		FraudPredictionMessage prediction = msg.predictionMessage();

		String contributingFeatures = formatContributingFeatures(
				prediction.explainability().topContributingFeatures());

		return """
				# Fraud Alert — Transaction %s

				---

				## Transaction Details

				- **Transaction ID:** %s
				- **User ID:** %s
				- **Card ID:** %s
				- **Merchant ID:** %s
				- **Device ID:** %s
				- **Amount:** $%s
				- **Country:** %s
				- **IP Address:** %s
				- **Timestamp:** %s

				---

				## Risk Assessment

				- **Risk Level:** %s
				- **Fraud Probability:** %.2f%%
				- **Model Version:** %s

				---

				## Fraud Detection Features

				- **User Average Amount:** $%s
				- **Amount-to-Average Ratio:** %.2f
				- **Transactions (last 5 min):** %d
				- **Transactions (last 1 hour):** %d
				- **Seconds Since Last Transaction:** %d
				- **Merchant Risk Score:** %.2f
				- **IP Risk Score:** %.2f
				- **Device Trusted:** %s
				- **Country Mismatch:** %s
				- **Card Age (days):** %d
				- **Hour of Day:** %d

				---

				## Top Contributing Features

				%s

				---

				## AI Analysis

				%s
				""".formatted(
				msg.transactionId(),
				msg.transactionId(),
				msg.userId(),
				msg.cardId(),
				msg.merchantId(),
				msg.deviceId(),
				msg.amount(),
				msg.countryCode(),
				msg.ipAddress(),
				msg.creationDateTime(),
				prediction.riskLevel(),
				prediction.fraudProbability() * 100,
				prediction.modelVersion(),
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
				contributingFeatures,
				llmExplanation
		);
	}

	private static String formatContributingFeatures(List<FeatureContributionMessage> features) {
		return features.stream()
				.map(f -> "- **%s:** %s (contribution: %.4f, direction: %s)".formatted(
						f.featureName(), f.featureValue(), f.contribution(), f.direction()))
				.collect(Collectors.joining("\n"));
	}
}
