package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pdzsoftware.riskactionhandler.domain.exception.LlmClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.LlmRestClientProperties;
import org.pdzsoftware.riskactionhandler.support.TestConstants;
import org.pdzsoftware.riskactionhandler.support.TestFixtures;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.CHAT_COMPLETIONS_ENDPOINT;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.LLM_BASE_URL;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.LLM_ERROR_MESSAGE_PREFIX;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.LLM_EXPLANATION;
import static org.pdzsoftware.riskactionhandler.support.TestConstants.TRANSACTION_ID;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LlmClientTest {

    private MockRestServiceServer mockServer;
    private LlmClient llmClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(LLM_BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        LlmRestClientProperties properties = new LlmRestClientProperties();
        properties.setModel(TestConstants.LLM_MODEL);
        properties.setTemperature(0.2);
        properties.setMaxCompletionTokens(512);
        llmClient = new LlmClient(builder.build(), properties);
    }

    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    @Test
    void getFraudExplanation_shouldReturnCompletionContent_whenCallSucceeds() {
        mockServer.expect(requestTo(LLM_BASE_URL + CHAT_COMPLETIONS_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(successfulCompletionJson(), MediaType.APPLICATION_JSON));

        String explanation = llmClient.getFraudExplanation(TestFixtures.highRiskTransaction());

        assertThat(explanation).isEqualTo(LLM_EXPLANATION);
    }

    @Test
    void getFraudExplanation_shouldThrow_whenResponseHasNoChoices() {
        mockServer.expect(requestTo(LLM_BASE_URL + CHAT_COMPLETIONS_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> llmClient.getFraudExplanation(TestFixtures.highRiskTransaction()))
                .isInstanceOf(LlmClientException.class)
                .hasMessage(TestConstants.LLM_NO_CHOICES_MESSAGE);
    }

    @Test
    void getFraudExplanation_shouldThrow_whenRestClientFails() {
        mockServer.expect(requestTo(LLM_BASE_URL + CHAT_COMPLETIONS_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> llmClient.getFraudExplanation(TestFixtures.highRiskTransaction()))
                .isInstanceOf(LlmClientException.class)
                .hasMessageContaining(LLM_ERROR_MESSAGE_PREFIX)
                .hasMessageContaining(TRANSACTION_ID);
    }

    private static String successfulCompletionJson() {
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "%s"
                      }
                    }
                  ]
                }
                """.formatted(LLM_EXPLANATION);
    }
}
