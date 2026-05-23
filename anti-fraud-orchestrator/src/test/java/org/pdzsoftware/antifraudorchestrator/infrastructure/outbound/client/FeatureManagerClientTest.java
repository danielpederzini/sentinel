package org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pdzsoftware.antifraudorchestrator.domain.exception.FeatureManagerClientException;
import org.pdzsoftware.antifraudorchestrator.infrastructure.inbound.consumer.dto.TransactionCreatedMessage;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.FraudFeatureResponse;
import org.pdzsoftware.antifraudorchestrator.infrastructure.outbound.client.dto.PersistTransactionRequest;
import org.pdzsoftware.antifraudorchestrator.support.TestFixtures;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.FEATURE_MANAGER_ERROR_MESSAGE;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.FRAUD_FEATURES_ENDPOINT;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.PERSIST_TRANSACTION_ENDPOINT;
import static org.pdzsoftware.antifraudorchestrator.support.TestConstants.TRANSACTION_ID;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FeatureManagerClientTest {

    private static final String BASE_URL = "http://feature-manager";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private MockRestServiceServer mockServer;
    private FeatureManagerClient featureManagerClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        featureManagerClient = new FeatureManagerClient(builder.build());
    }

    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    @Test
    void calculateFraudFeatures_shouldReturnResponse_whenCallSucceeds() throws Exception {
        FraudFeatureResponse expected = TestFixtures.fraudFeatureResponse();
        mockServer.expect(requestTo(BASE_URL + FRAUD_FEATURES_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(OBJECT_MAPPER.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        FraudFeatureResponse result = featureManagerClient.calculateFraudFeatures(TestFixtures.transactionCreatedMessage());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void calculateFraudFeatures_shouldThrow_whenResponseBodyIsNull() {
        mockServer.expect(requestTo(BASE_URL + FRAUD_FEATURES_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        TransactionCreatedMessage message = TestFixtures.transactionCreatedMessage();

        assertThatThrownBy(() -> featureManagerClient.calculateFraudFeatures(message))
                .isInstanceOf(FeatureManagerClientException.class)
                .hasMessage(FEATURE_MANAGER_ERROR_MESSAGE);
    }

    @Test
    void calculateFraudFeatures_shouldThrow_whenRestClientFails() {
        mockServer.expect(requestTo(BASE_URL + FRAUD_FEATURES_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        TransactionCreatedMessage message = TestFixtures.transactionCreatedMessage();

        assertThatThrownBy(() -> featureManagerClient.calculateFraudFeatures(message))
                .isInstanceOf(FeatureManagerClientException.class)
                .hasMessage(FEATURE_MANAGER_ERROR_MESSAGE)
                .cause()
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void persistTransaction_shouldComplete_whenCallSucceeds() {
        mockServer.expect(requestTo(BASE_URL + PERSIST_TRANSACTION_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        PersistTransactionRequest request = PersistTransactionRequest.from(
                TestFixtures.transactionCreatedMessage(),
                TestFixtures.fraudFeatureResponse(),
                TestFixtures.fraudPredictionResponse());

        featureManagerClient.persistTransaction(request);
    }

    @Test
    void persistTransaction_shouldThrow_whenRestClientFails() {
        mockServer.expect(requestTo(BASE_URL + PERSIST_TRANSACTION_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        PersistTransactionRequest request = PersistTransactionRequest.from(
                TestFixtures.transactionCreatedMessage(),
                TestFixtures.fraudFeatureResponse(),
                TestFixtures.fraudPredictionResponse());

        assertThatThrownBy(() -> featureManagerClient.persistTransaction(request))
                .isInstanceOf(FeatureManagerClientException.class)
                .hasMessageContaining(TRANSACTION_ID);
    }
}
