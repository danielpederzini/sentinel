package org.pdzsoftware.transactioningestor.infrastructure.inbound.controller;

import org.junit.jupiter.api.Test;
import org.pdzsoftware.transactioningestor.application.service.TransactionIngestionService;
import org.pdzsoftware.transactioningestor.domain.exception.KafkaPublishException;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionResponse;
import org.pdzsoftware.transactioningestor.support.TestFixtures;
import org.pdzsoftware.transactioningestor.domain.exception.handler.GlobalExceptionHandler;
import org.pdzsoftware.transactioningestor.infrastructure.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.pdzsoftware.transactioningestor.support.TestConstants.HTTP_STATUS_ACCEPTED;
import static org.pdzsoftware.transactioningestor.support.TestConstants.HTTP_STATUS_BAD_REQUEST;
import static org.pdzsoftware.transactioningestor.support.TestConstants.HTTP_STATUS_INTERNAL_SERVER_ERROR;
import static org.pdzsoftware.transactioningestor.support.TestConstants.KAFKA_PUBLISH_ERROR_MESSAGE;
import static org.pdzsoftware.transactioningestor.support.TestConstants.TRANSACTIONS_API_PATH;
import static org.pdzsoftware.transactioningestor.support.TestConstants.UNEXPECTED_ERROR_MESSAGE;

@WebFluxTest(controllers = TransactionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class TransactionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TransactionIngestionService transactionIngestionService;

    @Test
    void ingestTransaction_shouldReturnAccepted_whenServiceSucceeds() {
        TransactionIngestionResponse response = TestFixtures.ingestionResponse();
        when(transactionIngestionService.ingest(any())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(TRANSACTIONS_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequestJson())
                .exchange()
                .expectStatus().isEqualTo(HTTP_STATUS_ACCEPTED)
                .expectBody()
                .jsonPath("$.transactionId").isEqualTo(response.transactionId());
    }

    @Test
    void ingestTransaction_shouldReturnBadRequest_whenValidationFails() {
        webTestClient.post()
                .uri(TRANSACTIONS_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequestJson())
                .exchange()
                .expectStatus().isEqualTo(HTTP_STATUS_BAD_REQUEST);
    }

    @Test
    void ingestTransaction_shouldReturnInternalServerError_whenServiceFails() {
        when(transactionIngestionService.ingest(any()))
                .thenReturn(Mono.error(new KafkaPublishException(KAFKA_PUBLISH_ERROR_MESSAGE, new RuntimeException(UNEXPECTED_ERROR_MESSAGE))));

        webTestClient.post()
                .uri(TRANSACTIONS_API_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequestJson())
                .exchange()
                .expectStatus().is5xxServerError();
    }

    private static String validRequestJson() {
        return """
                {
                  "transactionId": "txn-1",
                  "userId": "user-1",
                  "cardId": "card-1",
                  "merchantId": "merchant-1",
                  "deviceId": "device-1",
                  "amount": 99.99,
                  "countryCode": "US",
                  "ipAddress": "203.0.113.1",
                  "creationDateTime": "2024-06-15T12:00:00"
                }
                """;
    }

    private static String invalidRequestJson() {
        return """
                {
                  "transactionId": "txn-1",
                  "userId": "",
                  "cardId": "card-1",
                  "merchantId": "merchant-1",
                  "amount": 99.99,
                  "countryCode": "US",
                  "creationDateTime": "2024-06-15T12:00:00"
                }
                """;
    }
}
