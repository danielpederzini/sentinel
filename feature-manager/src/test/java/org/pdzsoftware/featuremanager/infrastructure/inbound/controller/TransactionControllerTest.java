package org.pdzsoftware.featuremanager.infrastructure.inbound.controller;

import org.junit.jupiter.api.Test;
import org.pdzsoftware.featuremanager.application.usecase.PersistTransactionUseCase;
import org.pdzsoftware.featuremanager.domain.exception.UserNotFoundException;
import org.pdzsoftware.featuremanager.domain.exception.handler.GlobalExceptionHandler;
import org.pdzsoftware.featuremanager.infrastructure.config.SecurityConfig;
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
import static org.pdzsoftware.featuremanager.support.TestConstants.HTTP_STATUS_BAD_REQUEST;
import static org.pdzsoftware.featuremanager.support.TestConstants.HTTP_STATUS_CREATED;
import static org.pdzsoftware.featuremanager.support.TestConstants.HTTP_STATUS_NOT_FOUND;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTION_ID_1;
import static org.pdzsoftware.featuremanager.support.TestConstants.TRANSACTIONS_API_PATH;
import static org.pdzsoftware.featuremanager.support.TestConstants.USER_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersistTransactionUseCase persistTransactionUseCase;

    @Test
    void persistTransaction_shouldReturnCreated_whenRequestIsValid() throws Exception {
        when(persistTransactionUseCase.execute(any())).thenReturn(TRANSACTION_ID_1);

        mockMvc.perform(post(TRANSACTIONS_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPersistTransactionRequestJson()))
                .andExpect(status().is(HTTP_STATUS_CREATED));

        verify(persistTransactionUseCase).execute(any());
    }

    @Test
    void persistTransaction_shouldReturnBadRequest_whenValidationFails() throws Exception {
        mockMvc.perform(post(TRANSACTIONS_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPersistTransactionRequestJson()))
                .andExpect(status().is(HTTP_STATUS_BAD_REQUEST));
    }

    @Test
    void persistTransaction_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        when(persistTransactionUseCase.execute(any()))
                .thenThrow(new UserNotFoundException("User with id " + USER_ID + " not found"));

        mockMvc.perform(post(TRANSACTIONS_API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPersistTransactionRequestJson()))
                .andExpect(status().is(HTTP_STATUS_NOT_FOUND));
    }

    private static String validPersistTransactionRequestJson() {
        return """
                {
                  "transactionId": "txn-1",
                  "userId": "user-1",
                  "cardId": "card-1",
                  "merchantId": "merchant-1",
                  "deviceId": "device-1",
                  "amount": 50,
                  "countryCode": "US",
                  "ipAddress": "203.0.113.1",
                  "creationDateTime": "2024-06-15T12:00:00",
                  "features": {
                    "userAverageAmount": 100,
                    "userHistoricalTransactionCount": 5,
                    "userTransactionCount5Min": 1,
                    "userTransactionCount1Hour": 2,
                    "secondsSinceLastTransaction": 3600,
                    "merchantRiskScore": 0.2,
                    "isDeviceTrusted": true,
                    "hasCountryMismatch": false,
                    "amountToAverageRatio": 0.5,
                    "hourOfDay": 14,
                    "ipRiskScore": 0.1,
                    "cardAgeDays": 30,
                    "amountVelocity1Hour": 10,
                    "userAccountAgeDays": 365,
                    "dayOfWeek": 3,
                    "merchantCategory": 1,
                    "cardType": 0,
                    "distinctMerchantCount1Hour": 1,
                    "logAmount": 3.9,
                    "logSecondsSinceLastTransaction": 8.0,
                    "logVelocity1Hour": 2.3,
                    "amountTimesMerchantRisk": 10.0,
                    "riskScoreProduct": 0.02,
                    "ipDeviceRisk": 0.0,
                    "countryIpRisk": 0.0,
                    "velocityAmountInteraction": 1.0,
                    "recencyVelocity": 0.5,
                    "amountDeviation": 0.1,
                    "isNight": false,
                    "velocityIntensity": 5.0
                  },
                  "prediction": {
                    "fraudProbability": 0.15,
                    "riskLevel": "LOW",
                    "modelVersion": "v1"
                  }
                }
                """;
    }

    private static String invalidPersistTransactionRequestJson() {
        return """
                {
                  "transactionId": "txn-1",
                  "userId": "",
                  "cardId": "card-1",
                  "merchantId": "merchant-1",
                  "amount": 50,
                  "countryCode": "US",
                  "creationDateTime": "2024-06-15T12:00:00",
                  "features": {
                    "userAverageAmount": 100,
                    "userHistoricalTransactionCount": 5,
                    "userTransactionCount5Min": 1,
                    "userTransactionCount1Hour": 2,
                    "secondsSinceLastTransaction": 3600,
                    "merchantRiskScore": 0.2,
                    "isDeviceTrusted": true,
                    "hasCountryMismatch": false,
                    "amountToAverageRatio": 0.5,
                    "hourOfDay": 14,
                    "ipRiskScore": 0.1,
                    "cardAgeDays": 30,
                    "amountVelocity1Hour": 10,
                    "userAccountAgeDays": 365,
                    "dayOfWeek": 3,
                    "merchantCategory": 1,
                    "cardType": 0,
                    "distinctMerchantCount1Hour": 1,
                    "logAmount": 3.9,
                    "logSecondsSinceLastTransaction": 8.0,
                    "logVelocity1Hour": 2.3,
                    "amountTimesMerchantRisk": 10.0,
                    "riskScoreProduct": 0.02,
                    "ipDeviceRisk": 0.0,
                    "countryIpRisk": 0.0,
                    "velocityAmountInteraction": 1.0,
                    "recencyVelocity": 0.5,
                    "amountDeviation": 0.1,
                    "isNight": false,
                    "velocityIntensity": 5.0
                  },
                  "prediction": {
                    "fraudProbability": 0.15,
                    "riskLevel": "LOW",
                    "modelVersion": "v1"
                  }
                }
                """;
    }
}
