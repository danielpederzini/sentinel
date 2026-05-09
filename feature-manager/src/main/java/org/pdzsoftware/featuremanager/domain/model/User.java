package org.pdzsoftware.featuremanager.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

public abstract class User {
    protected String id;
    protected String email;
    protected LocalDate birthDate;
    protected Locale.IsoCountryCode homeCountryCode;
    protected LocalDateTime creationDateTime;
}
