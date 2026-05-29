package org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Acknowledgement returned after a transaction is accepted")
public record TransactionIngestionResponse(
        @Schema(description = "Identifier of the accepted transaction", example = "txn-1029384756")
        String transactionId
) {
}
