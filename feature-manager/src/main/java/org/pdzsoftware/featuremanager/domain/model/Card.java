package org.pdzsoftware.featuremanager.domain.model;

import lombok.*;
import org.pdzsoftware.featuremanager.domain.enums.CardType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Card {
    private String id;
    private CardType type;
    private LocalDateTime creationDateTime;
}
