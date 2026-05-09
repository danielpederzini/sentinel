package org.pdzsoftware.featuremanager.domain.model;

import org.pdzsoftware.featuremanager.domain.enums.DeviceType;

import java.time.LocalDateTime;

public abstract class TrustedDevice {
    protected String id;
    protected String name;
    protected DeviceType deviceType;
    protected LocalDateTime creationDateTime;
}
