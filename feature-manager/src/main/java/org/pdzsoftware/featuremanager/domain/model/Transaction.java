package org.pdzsoftware.featuremanager.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

public abstract class Transaction {
    protected String id;
    protected BigDecimal amount;
    protected Locale.IsoCountryCode countryCode;
    protected String ipAddress;
    protected LocalDateTime creationDateTime;
}
