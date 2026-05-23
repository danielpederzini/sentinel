package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pdzsoftware.antifraudorchestrator.domain.exception.InferenceEngineClientException;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudPredictionResponse;
import org.pdzsoftware.antifraudorchestrator.support.TestFixtures;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.INFERENCE_ERROR_MESSAGE;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.INFERENCE_SCORE_ENDPOINT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InferenceEngineClientTest {

    private static final String BASE_URL = "http://inference-engine";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private MockRestServiceServer mockServer;
    private InferenceEngineClient inferenceEngineClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        inferenceEngineClient = new InferenceEngineClient(builder.build());
    }

    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    @Test
    void scoreTransaction_shouldReturnResponse_whenCallSucceeds() throws Exception {
        FraudPredictionResponse expected = TestFixtures.fraudPredictionResponse();
        mockServer.expect(requestTo(BASE_URL + INFERENCE_SCORE_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(OBJECT_MAPPER.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        FraudFeatureResponse features = TestFixtures.fraudFeatureResponse();

        FraudPredictionResponse result = inferenceEngineClient.scoreTransaction(features);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void scoreTransaction_shouldThrow_whenResponseBodyIsNull() {
        mockServer.expect(requestTo(BASE_URL + INFERENCE_SCORE_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        FraudFeatureResponse features = TestFixtures.fraudFeatureResponse();

        assertThatThrownBy(() -> inferenceEngineClient.scoreTransaction(features))
                .isInstanceOf(InferenceEngineClientException.class)
                .hasMessage(INFERENCE_ERROR_MESSAGE);
    }

    @Test
    void scoreTransaction_shouldThrow_whenRestClientFails() {
        mockServer.expect(requestTo(BASE_URL + INFERENCE_SCORE_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        FraudFeatureResponse features = TestFixtures.fraudFeatureResponse();

        assertThatThrownBy(() -> inferenceEngineClient.scoreTransaction(features))
                .isInstanceOf(InferenceEngineClientException.class)
                .hasMessage(INFERENCE_ERROR_MESSAGE)
                .cause()
                .isInstanceOf(RestClientException.class);
    }
}
