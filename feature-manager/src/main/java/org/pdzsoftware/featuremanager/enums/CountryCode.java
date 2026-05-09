package org.pdzsoftware.featuremanager.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CountryCode {
    AR("Argentina"),
    AU("Australia"),
    BR("Brazil"),
    CA("Canada"),
    CH("Switzerland"),
    CL("Chile"),
    CN("China"),
    DE("Germany"),
    ES("Spain"),
    FR("France"),
    GB("United Kingdom"),
    IN("India"),
    IT("Italy"),
    JP("Japan"),
    MX("Mexico"),
    NL("Netherlands"),
    PT("Portugal"),
    SE("Sweden"),
    US("United States"),
    ZA("South Africa");

    private final String fullName;
}
