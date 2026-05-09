package org.pdzsoftware.featuremanager.domain.model;

import org.pdzsoftware.featuremanager.domain.enums.DeviceType;

import java.time.LocalDateTime;

public class TrustedDevice {
    private String id;
    private String name;
    private DeviceType type;
    private LocalDateTime creationDateTime;

    public TrustedDevice(String id,
                         String name,
                         DeviceType type,
                         LocalDateTime creationDateTime) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.creationDateTime = creationDateTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public LocalDateTime getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(LocalDateTime creationDateTime) {
        this.creationDateTime = creationDateTime;
    }
}
