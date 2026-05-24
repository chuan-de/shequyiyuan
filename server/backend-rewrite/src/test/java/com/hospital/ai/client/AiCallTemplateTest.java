package com.hospital.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.hospital.ai.client.AiCallTemplate.RawResponse;
import com.hospital.ai.config.AiProperties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class AiCallTemplateTest {

    private AiCallTemplate template() {
        AiProperties props = new AiProperties();
        props.setEnabled(true);
        props.setBaseUrl("https://example.invalid/api/v3");
        props.setApiKey("dummy");
        props.setTimeoutSeconds(5);
        // RestClient + streaming deps are unused by the test seam below — pass empty providers.
        return new AiCallTemplate(
                RestClient.builder().build(), props,
                empty(), empty(), empty(), empty());
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> empty() {
        return (ObjectProvider<T>) new ObjectProvider<Object>() {
            @Override public Object getObject(Object... args) { return null; }
            @Override public Object getObject() { return null; }
            @Override public Object getIfAvailable() { return null; }
            @Override public Object getIfUnique() { return null; }
        };
    }

    @Test
    void chat_alwaysForcesTemperatureToOne_evenIfRequestOmitsIt() {
        AiCallTemplate t = template();
        ChatRequest req = ChatRequest.builder()
                .feature("consult")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", "hi")))
                .build();

        Map<String, Object> body = t.buildBody(req);

        assertThat(body).containsEntry("temperature", 1);
        assertThat(body).containsEntry("model", "doubao-seed-2.0-lite");
    }

    @Test
    void chat_returnsParsedContentAndTokenUsage() {
        AiCallTemplate t = template();
        ChatRequest req = ChatRequest.builder()
                .feature("vision")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", "describe")))
                .build();

        ChatResponse resp = t.chatWith(req, body -> new RawResponse(Map.of(
                "model", "doubao-seed-2.0-lite",
                "choices", List.of(Map.of(
                        "message", Map.of("role", "assistant", "content", "hello world")
                )),
                "usage", Map.of("prompt_tokens", 12, "completion_tokens", 8)
        )));

        assertThat(resp.content()).isEqualTo("hello world");
        assertThat(resp.tokensIn()).isEqualTo(12);
        assertThat(resp.tokensOut()).isEqualTo(8);
        assertThat(resp.totalTokens()).isEqualTo(20);
        assertThat(resp.model()).isEqualTo("doubao-seed-2.0-lite");
    }

    @Test
    void chat_retriesOn429ThenSucceeds() {
        AiCallTemplate t = template();
        ChatRequest req = ChatRequest.builder()
                .feature("consult")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", "x")))
                .build();
        AtomicInteger attempts = new AtomicInteger();

        ChatResponse resp = t.chatWith(req, body -> {
            int n = attempts.incrementAndGet();
            if (n < 3) {
                throw httpError(HttpStatus.TOO_MANY_REQUESTS);
            }
            return new RawResponse(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", "ok"))),
                    "usage", Map.of("prompt_tokens", 1, "completion_tokens", 1)
            ));
        });

        assertThat(attempts.get()).isEqualTo(3);
        assertThat(resp.content()).isEqualTo("ok");
    }

    @Test
    void chat_givesUpAfterThreeAttemptsOn5xx() {
        AiCallTemplate t = template();
        ChatRequest req = ChatRequest.builder()
                .feature("consult")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", "x")))
                .build();
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> t.chatWith(req, body -> {
            attempts.incrementAndGet();
            throw httpError(HttpStatus.BAD_GATEWAY);
        })).isInstanceOf(RestClientResponseException.class);

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void chat_doesNotRetryOn4xxOtherThan429() {
        AiCallTemplate t = template();
        ChatRequest req = ChatRequest.builder()
                .feature("consult")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", "x")))
                .build();
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> t.chatWith(req, body -> {
            attempts.incrementAndGet();
            throw httpError(HttpStatus.BAD_REQUEST);
        })).isInstanceOf(RestClientResponseException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void chatStream_errorsWhenWebClientUnavailable() {
        AiCallTemplate t = template();
        ChatRequest req = ChatRequest.builder()
                .feature("consult")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", "x")))
                .build();

        // No WebClient provider in this test wiring → first signal must be an error.
        assertThatThrownBy(() -> t.chatStream(req).blockFirst())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aiWebClient");
    }

    private static RestClientResponseException httpError(HttpStatusCode status) {
        return new RestClientResponseException(
                "upstream " + status.value(),
                status,
                status.toString(),
                null,
                "{}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }
}
