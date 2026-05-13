package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto;

import java.util.List;

public record ChatCompletionResponse(List<ChatCompletionChoice> choices) {
}
