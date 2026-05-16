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

		String contributingFeaturesRows = contributions.stream()
				.map(feature -> "<tr><td>%s</td><td>%s</td><td>%.4f</td><td>%s</td></tr>".formatted(
						feature.featureName(), feature.featureValue(), feature.contribution(), feature.direction()))
				.collect(Collectors.joining("\n"));

		return """
				<html>
				<body style="font-family: Arial, sans-serif; color: #333; max-width: 700px; margin: auto;">
				  <h1 style="color: #b30000;">Fraud Alert &mdash; Transaction %s</h1>
				  <hr>

				  <h2>Transaction Details</h2>
				  <table style="border-collapse: collapse; width: 100%%;">
				    <tr><td style="padding: 4px 12px;"><strong>Transaction ID</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>User ID</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Card ID</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Merchant ID</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Device ID</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Amount</strong></td><td>$%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Country</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>IP Address</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Timestamp</strong></td><td>%s</td></tr>
				  </table>
				  <hr>

				  <h2>Risk Assessment</h2>
				  <table style="border-collapse: collapse; width: 100%%;">
				    <tr><td style="padding: 4px 12px;"><strong>Risk Level</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Fraud Probability</strong></td><td>%.2f%%</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Model Version</strong></td><td>%s</td></tr>
				  </table>
				  <hr>

				  <h2>Fraud Detection Features</h2>
				  <table style="border-collapse: collapse; width: 100%%;">
				    <tr><td style="padding: 4px 12px;"><strong>User Average Amount</strong></td><td>$%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Amount-to-Average Ratio</strong></td><td>%.2f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Transactions (last 5 min)</strong></td><td>%d</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Transactions (last 1 hour)</strong></td><td>%d</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Seconds Since Last Transaction</strong></td><td>%d</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Merchant Risk Score</strong></td><td>%.2f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>IP Risk Score</strong></td><td>%.2f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Device Trusted</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Country Mismatch</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Card Age (days)</strong></td><td>%d</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Hour of Day</strong></td><td>%d</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Amount Velocity (1 Hour)</strong></td><td>$%s</td></tr>
				  </table>
				  <hr>

				  <h2>Engineered Features</h2>
				  <table style="border-collapse: collapse; width: 100%%;">
				    <tr><td style="padding: 4px 12px;"><strong>Log Amount</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Log Seconds Since Last Transaction</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Log Velocity (1 Hour)</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Amount &times; Merchant Risk</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Amount &times; IP Risk</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Risk Score Product</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>IP Device Risk</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Country IP Risk</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Velocity Amount Interaction</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Recency Velocity</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Card Age &times; Amount Ratio</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Amount Deviation</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Is Night</strong></td><td>%s</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Night Amount Ratio</strong></td><td>%.4f</td></tr>
				    <tr><td style="padding: 4px 12px;"><strong>Velocity Intensity</strong></td><td>%.4f</td></tr>
				  </table>
				  <hr>

				  <h2>Top Contributing Features</h2>
				  <table style="border-collapse: collapse; width: 100%%;">
				    <tr style="background-color: #f2f2f2;">
				      <th style="padding: 6px 12px; text-align: left;">Feature</th>
				      <th style="padding: 6px 12px; text-align: left;">Value</th>
				      <th style="padding: 6px 12px; text-align: left;">Contribution</th>
				      <th style="padding: 6px 12px; text-align: left;">Direction</th>
				    </tr>
				    %s
				  </table>
				  <hr>

				  <h2>AI Analysis</h2>
				  <p>%s</p>
				</body>
				</html>
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
				features.amountVelocity1Hour(),
				features.logAmount(),
				features.logSecondsSinceLastTransaction(),
				features.logVelocity1Hour(),
				features.amountTimesMerchantRisk(),
				features.amountTimesIpRisk(),
				features.riskScoreProduct(),
				features.ipDeviceRisk(),
				features.countryIpRisk(),
				features.velocityAmountInteraction(),
				features.recencyVelocity(),
				features.cardAgeAmountRatio(),
				features.amountDeviation(),
				features.isNight(),
				features.nightAmountRatio(),
				features.velocityIntensity(),
				contributingFeaturesRows,
				llmExplanation
		);
	}
}
