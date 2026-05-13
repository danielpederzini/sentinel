package org.pdzsoftware.riskactionhandler.infrastructure.outbound.client;

import org.pdzsoftware.riskactionhandler.domain.exception.LlmClientException;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.LlmRestClientProperties;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatCompletionRequest;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatCompletionResponse;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.client.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;

@Component
public class LlmClient {
	private static final String ROLE_NAME = "user";
	private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
	private static final String EXPLAIN_FRAUD_PROMPT = """
			Given the following transaction details, explain why it might be considered fraudulent.
			Answer in a concise and assertive paragraph summarizing only the most important insights:
			""";
	private final RestClient restClient;
	private final LlmRestClientProperties properties;

	public LlmClient(@Qualifier("llmRestClient") RestClient restClient, LlmRestClientProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public String getFraudExplanation(TransactionScoredMessage transactionScoredMessage) {
		try {
			ChatMessage chatMessage = ChatMessage.builder()
					.role(ROLE_NAME)
					.content(EXPLAIN_FRAUD_PROMPT + transactionScoredMessage.toString())
					.build();

			ChatCompletionRequest request = ChatCompletionRequest.builder()
					.model(properties.getModel())
					.messages(List.of(chatMessage))
					.temperature(properties.getTemperature())
					.maxCompletionTokens(properties.getMaxCompletionTokens())
					.stream(false)
					.build();

			ChatCompletionResponse response = restClient.post()
					.uri(CHAT_COMPLETIONS_ENDPOINT)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(ChatCompletionResponse.class);

            Objects.requireNonNull(response, "LLM returned an empty response body");

            if (response.choices() == null || response.choices().isEmpty()) {
				throw new LlmClientException("LLM returned no choices");
			}

			ChatMessage llmMessage = response.choices().getFirst().message();
			if (llmMessage == null || !StringUtils.hasText(llmMessage.content())) {
				throw new LlmClientException("LLM returned an empty completion");
			}
			return llmMessage.content();
		} catch (RestClientException | NullPointerException exception) {
			throw new LlmClientException(String.format("Failed to get LLM explainability details for transaction %s",
					transactionScoredMessage.transactionId()), exception);
		}
	}
}
