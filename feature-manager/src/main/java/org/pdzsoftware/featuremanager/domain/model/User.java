package org.pdzsoftware.featuremanager.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String id;
    private String email;
    private LocalDate birthDate;
    private Locale.IsoCountryCode homeCountryCode;
    private LocalDateTime creationDateTime;
}
