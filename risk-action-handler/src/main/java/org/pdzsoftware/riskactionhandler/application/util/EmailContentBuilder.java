package org.pdzsoftware.riskactionhandler.application.util;

import lombok.experimental.UtilityClass;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudFeaturesMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.FraudPredictionMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@UtilityClass
public class EmailContentBuilder {

	private static final String TEMPLATE_PATH = "/templates/fraud-alert-email.html";
	private static final String EMAIL_TEMPLATE = loadTemplate();

	public static String buildFraudAlertEmail(TransactionScoredMessage message, String llmExplanation) {
		FraudFeaturesMessage features = message.featuresMessage();
		FraudPredictionMessage prediction = message.predictionMessage();

		String contributingFeaturesRows = prediction.topContributingFeatures().stream()
				.map(feature -> "<tr><td>%s</td><td>%s</td><td>%.4f</td><td>%s</td></tr>".formatted(
						feature.featureName(), feature.featureValue(), feature.contribution(), feature.direction()))
				.collect(Collectors.joining("\n"));

		return EMAIL_TEMPLATE.formatted(
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
				features.amountVelocity1Hour(),
				features.userAccountAgeDays(),
				features.dayOfWeek(),
				features.merchantCategory(),
				features.cardType(),
				features.distinctMerchantCount1Hour(),
				features.logAmount(),
				features.logSecondsSinceLastTransaction(),
				features.logVelocity1Hour(),
				features.amountTimesMerchantRisk(),
				features.riskScoreProduct(),
				features.ipDeviceRisk(),
				features.countryIpRisk(),
				features.velocityAmountInteraction(),
				features.recencyVelocity(),
				features.amountDeviation(),
				features.isNight(),
				features.velocityIntensity(),
				contributingFeaturesRows,
				llmExplanation
		);
	}

	private static String loadTemplate() {
		try (InputStream stream = EmailContentBuilder.class.getResourceAsStream(TEMPLATE_PATH)) {
			if (stream == null) {
				throw new IllegalStateException("Email template not found on classpath: " + TEMPLATE_PATH);
			}
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to load email template: " + TEMPLATE_PATH, exception);
		}
	}
}
