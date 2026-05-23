package org.pdzsoftware.transactioningestor.support;

import org.pdzsoftware.transactioningestor.domain.enums.CountryCode;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionRequest;
import org.pdzsoftware.transactioningestor.infrastructure.inbound.controller.dto.TransactionIngestionResponse;

import static org.pdzsoftware.transactioningestor.support.TestConstants.CARD_ID;
import static org.pdzsoftware.transactioningestor.support.TestConstants.CREATION_DATE_TIME;
import static org.pdzsoftware.transactioningestor.support.TestConstants.DEVICE_ID;
import static org.pdzsoftware.transactioningestor.support.TestConstants.IP_ADDRESS;
import static org.pdzsoftware.transactioningestor.support.TestConstants.MERCHANT_ID;
import static org.pdzsoftware.transactioningestor.support.TestConstants.TRANSACTION_AMOUNT;
import static org.pdzsoftware.transactioningestor.support.TestConstants.TRANSACTION_ID;
import static org.pdzsoftware.transactioningestor.support.TestConstants.USER_ID;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static TransactionIngestionRequest ingestionRequest(String transactionId) {
        return new TransactionIngestionRequest(
                transactionId,
                USER_ID,
                CARD_ID,
                MERCHANT_ID,
                DEVICE_ID,
                TRANSACTION_AMOUNT,
                CountryCode.US,
                IP_ADDRESS,
                CREATION_DATE_TIME
        );
    }

    public static TransactionIngestionRequest ingestionRequestWithId() {
        return ingestionRequest(TRANSACTION_ID);
    }

    public static TransactionIngestionResponse ingestionResponse() {
        return new TransactionIngestionResponse(TRANSACTION_ID);
    }
}
