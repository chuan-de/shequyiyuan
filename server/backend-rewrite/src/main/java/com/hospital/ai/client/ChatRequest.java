package com.hospital.ai.client;

import java.util.List;
import java.util.Objects;

/**
 * Public request DTO for {@link AiCallTemplate#chat(ChatRequest)}.
 *
 * <p>Note: callers MUST NOT supply a temperature. Volcengine Ark rejects any
 * value other than 1, so {@link AiCallTemplate} always sets it to 1 itself.
 * That decision is centralised so individual features can never get it
 * wrong.</p>
 */
public final class ChatRequest {

    /** {@code vision} / {@code patient-rag} / {@code consult}. Used for audit. */
    private final String feature;
    private final String model;
    private final List<ChatMessage> messages;
    private final Integer maxTokens;
    /** Optional estimate used for rate-limit budgeting. */
    private final Integer estimatedTokens;

    private ChatRequest(Builder b) {
        this.feature = Objects.requireNonNull(b.feature, "feature");
        this.model = Objects.requireNonNull(b.model, "model");
        this.messages = List.copyOf(Objects.requireNonNull(b.messages, "messages"));
        this.maxTokens = b.maxTokens;
        this.estimatedTokens = b.estimatedTokens;
    }

    public String getFeature() { return feature; }
    public String getModel() { return model; }
    public List<ChatMessage> getMessages() { return messages; }
    public Integer getMaxTokens() { return maxTokens; }
    public Integer getEstimatedTokens() { return estimatedTokens; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String feature;
        private String model;
        private List<ChatMessage> messages;
        private Integer maxTokens;
        private Integer estimatedTokens;

        public Builder feature(String feature) { this.feature = feature; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder messages(List<ChatMessage> messages) { this.messages = messages; return this; }
        public Builder maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder estimatedTokens(Integer estimatedTokens) { this.estimatedTokens = estimatedTokens; return this; }

        public ChatRequest build() { return new ChatRequest(this); }
    }
}
