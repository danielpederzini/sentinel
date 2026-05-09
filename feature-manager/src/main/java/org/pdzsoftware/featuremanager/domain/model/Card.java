package org.pdzsoftware.featuremanager.domain.model;

import org.pdzsoftware.featuremanager.domain.enums.CardType;

import java.time.LocalDateTime;

public abstract class Card {
    protected String id;
    protected CardType type;
    protected LocalDateTime creationDateTime;
}
