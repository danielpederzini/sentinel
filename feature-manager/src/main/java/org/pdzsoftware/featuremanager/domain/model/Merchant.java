package org.pdzsoftware.featuremanager.domain.model;

import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;

import java.time.LocalDateTime;

public class Merchant {
    private String id;
    private String email;
    private float riskScore;
    private MerchantCategory category;
    private LocalDateTime creationDateTime;

    public Merchant(String id,
                    String email,
                    float riskScore,
                    MerchantCategory category,
                    LocalDateTime creationDateTime) {
        this.id = id;
        this.email = email;
        this.riskScore = riskScore;
        this.category = category;
        this.creationDateTime = creationDateTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public float getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(float riskScore) {
        this.riskScore = riskScore;
    }

    public MerchantCategory getCategory() {
        return category;
    }

    public void setCategory(MerchantCategory category) {
        this.category = category;
    }

    public LocalDateTime getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(LocalDateTime creationDateTime) {
        this.creationDateTime = creationDateTime;
    }
}
