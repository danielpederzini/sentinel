package org.pdzsoftware.riskactionhandler.application.util;

import org.junit.jupiter.api.Test;
import org.pdzsoftware.riskactionhandler.support.TestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.FEATURE_NAME_AMOUNT;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.HTML_AI_ANALYSIS_SECTION;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.HTML_FRAUD_ALERT_HEADING;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.LLM_EXPLANATION;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.TRANSACTION_ID;

class EmailContentBuilderTest {

    @Test
    void buildFraudAlertEmail_shouldIncludeTransactionDetailsAndLlmExplanation() {
        String email = EmailContentBuilder.buildFraudAlertEmail(
                TestFixtures.highRiskTransaction(),
                LLM_EXPLANATION);

        assertThat(email).contains(HTML_FRAUD_ALERT_HEADING);
        assertThat(email).contains(TRANSACTION_ID);
        assertThat(email).contains(FEATURE_NAME_AMOUNT);
        assertThat(email).contains(HTML_AI_ANALYSIS_SECTION);
        assertThat(email).contains(LLM_EXPLANATION);
    }

    @Test
    void buildFraudAlertEmail_shouldOmitContributingRows_whenExplainabilityIsNull() {
        String email = EmailContentBuilder.buildFraudAlertEmail(
                TestFixtures.highRiskTransactionWithoutExplainability(),
                LLM_EXPLANATION);

        assertThat(email).contains(LLM_EXPLANATION);
        assertThat(email).doesNotContain(FEATURE_NAME_AMOUNT);
    }
}
