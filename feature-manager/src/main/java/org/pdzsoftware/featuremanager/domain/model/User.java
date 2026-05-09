package org.pdzsoftware.featuremanager.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

public class User {
    private String id;
    private String email;
    private LocalDate birthDate;
    private Locale.IsoCountryCode homeCountryCode;
    private LocalDateTime creationDateTime;

    public User(String id,
                String email,
                LocalDate birthDate,
                Locale.IsoCountryCode homeCountryCode,
                LocalDateTime creationDateTime) {
        this.id = id;
        this.email = email;
        this.birthDate = birthDate;
        this.homeCountryCode = homeCountryCode;
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Locale.IsoCountryCode getHomeCountryCode() {
        return homeCountryCode;
    }

    public void setHomeCountryCode(Locale.IsoCountryCode homeCountryCode) {
        this.homeCountryCode = homeCountryCode;
    }

    public LocalDateTime getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(LocalDateTime creationDateTime) {
        this.creationDateTime = creationDateTime;
    }
}
