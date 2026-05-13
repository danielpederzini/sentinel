package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatCompletionRequest(
        @JsonProperty("model") String model,
        @JsonProperty("messages") List<ChatMessage> messages,
        @JsonProperty("temperature") double temperature,
        @JsonProperty("max_completion_tokens") int maxCompletionTokens,
        @JsonProperty("stream") boolean stream
) {
}
