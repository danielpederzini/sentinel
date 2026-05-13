package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto;

import lombok.Builder;

@Builder
public record ChatMessage(String role, String content) {
}
