package org.pdzsoftware.featuremanager.domain.model;

import org.pdzsoftware.featuremanager.domain.enums.MerchantCategory;

import java.time.LocalDateTime;

public abstract class Merchant {
    protected String id;
    protected String email;
    protected MerchantCategory category;
    protected LocalDateTime creationDateTime;
}
