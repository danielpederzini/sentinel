package org.pdzsoftware.featuremanager.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

public class Transaction {
    private String id;
    private BigDecimal amount;
    private Locale.IsoCountryCode countryCode;
    private String ipAddress;
    private LocalDateTime creationDateTime;

    public Transaction(String id,
                       BigDecimal amount,
                       Locale.IsoCountryCode countryCode,
                       String ipAddress,
                       LocalDateTime creationDateTime) {
        this.id = id;
        this.amount = amount;
        this.countryCode = countryCode;
        this.ipAddress = ipAddress;
        this.creationDateTime = creationDateTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Locale.IsoCountryCode getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(Locale.IsoCountryCode countryCode) {
        this.countryCode = countryCode;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(LocalDateTime creationDateTime) {
        this.creationDateTime = creationDateTime;
    }
}
