package com.hospital.ai.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.hospital.ai.config.AiProperties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for the SSE line parser in {@link AiCallTemplate#parseSseLine}.
 * The full streaming integration (real WebClient, real upstream) is too slow
 * to spin up here — we just verify the parser handles each frame shape the
 * Doubao gateway has been observed to send.
 */
class AiCallTemplateStreamTest {

    private AiCallTemplate template() {
        AiProperties props = new AiProperties();
        props.setEnabled(true);
        props.setBaseUrl("https://example.invalid/api/v3");
        props.setApiKey("dummy");
        props.setTimeoutSeconds(5);
        return new AiCallTemplate(
                RestClient.builder().build(), props,
                empty(), empty(), empty(), empty());
    }

    @Test
    void parsesDeltaContentFrame() {
        StringBuilder acc = new StringBuilder();
        AtomicReference<Integer> tin = new AtomicReference<>();
        AtomicReference<Integer> tout = new AtomicReference<>();
        AtomicInteger fallback = new AtomicInteger();

        ChatChunk chunk = template().parseSseLine(
                "data: {\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}",
                acc, tin, tout, fallback);

        assertThat(chunk).isNotNull();
        assertThat(chunk.done()).isFalse();
        assertThat(chunk.deltaContent()).isEqualTo("hello");
        assertThat(acc.toString()).isEqualTo("hello");
        assertThat(fallback.get()).isEqualTo(5);
    }

    @Test
    void recognisesDoneSentinel() {
        StringBuilder acc = new StringBuilder();
        AtomicReference<Integer> tin = new AtomicReference<>(11);
        AtomicReference<Integer> tout = new AtomicReference<>(22);
        ChatChunk chunk = template().parseSseLine("data: [DONE]", acc, tin, tout, new AtomicInteger());
        assertThat(chunk).isNotNull();
        assertThat(chunk.done()).isTrue();
        assertThat(chunk.tokensIn()).isEqualTo(11);
        assertThat(chunk.tokensOut()).isEqualTo(22);
    }

    @Test
    void ignoresBlankAndCommentLines() {
        StringBuilder acc = new StringBuilder();
        ChatChunk blank = template().parseSseLine("", acc, new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());
        ChatChunk comment = template().parseSseLine(":heartbeat", acc, new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());
        assertThat(blank).isNull();
        assertThat(comment).isNull();
    }

    @Test
    void capturesUsageBlockFromFinalFrame() {
        StringBuilder acc = new StringBuilder();
        AtomicReference<Integer> tin = new AtomicReference<>();
        AtomicReference<Integer> tout = new AtomicReference<>();
        ChatChunk chunk = template().parseSseLine(
                "data: {\"choices\":[{\"delta\":{}}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":34}}",
                acc, tin, tout, new AtomicInteger());
        // No content → null chunk emitted, but usage captured.
        assertThat(chunk).isNull();
        assertThat(tin.get()).isEqualTo(12);
        assertThat(tout.get()).isEqualTo(34);
    }

    @Test
    void malformedJsonIsSwallowedNotPropagated() {
        StringBuilder acc = new StringBuilder();
        ChatChunk chunk = template().parseSseLine(
                "data: {this is not json",
                acc, new AtomicReference<>(), new AtomicReference<>(), new AtomicInteger());
        assertThat(chunk).isNull();
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
}
