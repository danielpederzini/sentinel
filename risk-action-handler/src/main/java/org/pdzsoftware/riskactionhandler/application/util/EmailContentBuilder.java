package org.pdzsoftware.riskactionhandler.application.util;

import lombok.experimental.UtilityClass;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.ExplainabilityDetailsMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FeatureContributionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudFeaturesMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudPredictionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@UtilityClass
public class EmailContentBuilder {

	public static String buildFraudAlertEmail(TransactionScoredMessage message, String llmExplanation) {
		FraudFeaturesMessage features = message.featuresMessage();
		FraudPredictionMessage prediction = message.predictionMessage();

		List<FeatureContributionMessage> contributions = Optional.ofNullable(prediction.explainability())
				.map(ExplainabilityDetailsMessage::topContributingFeatures)
				.orElse(Collections.emptyList());

		String contributingFeatures = formatContributingFeatures(contributions);

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
				message.transactionId(),
				message.transactionId(),
				message.userId(),
				message.cardId(),
				message.merchantId(),
				message.deviceId(),
				message.amount(),
				message.countryCode(),
				message.ipAddress(),
				message.creationDateTime(),
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
				.map(feature -> "- **%s:** %s (contribution: %.4f, direction: %s)".formatted(
						feature.featureName(), feature.featureValue(), feature.contribution(), feature.direction()))
				.collect(Collectors.joining("\n"));
	}
}
