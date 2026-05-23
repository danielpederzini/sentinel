package org.pdzsoftware.featuremanager.infrastructure.inbound.controller;

import org.junit.jupiter.api.Test;
import org.pdzsoftware.featuremanager.application.usecase.CalculateFraudFeaturesUseCase;
import org.pdzsoftware.featuremanager.domain.exception.handler.GlobalExceptionHandler;
import org.pdzsoftware.featuremanager.infrastructure.config.SecurityConfig;
import org.pdzsoftware.featuremanager.infrastructure.inbound.controller.dto.FraudFeatureResponse;
import org.pdzsoftware.featuremanager.support.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.featuremanager.support.TestConstants.FRAUD_FEATURES_API_PATH;
import static org.pdzsoftware.featuremanager.support.TestConstants.HTTP_STATUS_BAD_REQUEST;
import static org.pdzsoftware.featuremanager.support.TestConstants.HTTP_STATUS_CREATED;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_1;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FraudFeatureController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class FraudFeatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalculateFraudFeaturesUseCase calculateFraudFeaturesUseCase;

    @Test
    void calculateFraudFeatures_shouldReturnCreated_whenRequestIsValid() throws Exception {
        FraudFeatureResponse response = TestFixtures.fraudFeatureResponse();
        when(calculateFraudFeaturesUseCase.execute(any())).thenReturn(response);

        mockMvc.perform(post(FRAUD_FEATURES_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFraudFeatureRequestJson()))
                .andExpect(status().is(HTTP_STATUS_CREATED))
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID_1));

        verify(calculateFraudFeaturesUseCase).execute(any());
    }

    @Test
    void calculateFraudFeatures_shouldReturnBadRequest_whenValidationFails() throws Exception {
        mockMvc.perform(post(FRAUD_FEATURES_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidFraudFeatureRequestJson()))
                .andExpect(status().is(HTTP_STATUS_BAD_REQUEST));
    }

    private static String validFraudFeatureRequestJson() {
        return """
                {
                  "transactionId": "txn-1",
                  "userId": "user-1",
                  "cardId": "card-1",
                  "merchantId": "merchant-1",
                  "deviceId": "device-1",
                  "amount": 200,
                  "countryCode": "US",
                  "ipAddress": "203.0.113.1",
                  "creationDateTime": "2024-06-15T14:00:00"
                }
                """;
    }

    private static String invalidFraudFeatureRequestJson() {
        return """
                {
                  "transactionId": "txn-1",
                  "userId": "",
                  "cardId": "card-1",
                  "merchantId": "merchant-1",
                  "amount": 200,
                  "countryCode": "US",
                  "creationDateTime": "2024-06-15T14:00:00"
                }
                """;
    }
}
