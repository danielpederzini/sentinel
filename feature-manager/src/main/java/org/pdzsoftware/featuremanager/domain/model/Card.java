package org.pdzsoftware.featuremanager.domain.model;

import org.pdzsoftware.featuremanager.domain.enums.CardType;

import java.time.LocalDateTime;

public class Card {
    private String id;
    private CardType type;
    private LocalDateTime creationDateTime;

    public Card(String id,
                CardType type,
                LocalDateTime creationDateTime) {
        this.id = id;
        this.type = type;
        this.creationDateTime = creationDateTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CardType getType() {
        return type;
    }

    public void setType(CardType type) {
        this.type = type;
    }

    public LocalDateTime getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(LocalDateTime creationDateTime) {
        this.creationDateTime = creationDateTime;
    }
}
