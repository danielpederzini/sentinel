package org.pdzsoftware.featuremanager.domain.model;

import java.time.LocalDateTime;

public abstract class TrustedDevice {
    protected String id;
    protected String name;
    protected LocalDateTime creationDateTime;
}
