package com.hospital.ai.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Wires the cross-cutting AI infrastructure: the shared {@link RestClient} used
 * by {@code AiCallTemplate} to call Doubao via its OpenAI-compatible REST API.
 *
 * <p>The whole configuration is gated by {@code hospital.ai.enabled=true}. When
 * the flag is false (e.g. CI without an API key) none of these beans are
 * created — see CLAUDE.md acceptance criterion #5.</p>
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
@EnableAsync
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class AiConfig {

    /**
     * Shared {@link RestClient} for AI provider calls. We deliberately do NOT
     * inject the base URL or auth header here — {@code AiCallTemplate} owns the
     * full request shape (URI, headers, retry, timeout) so audit + rate-limit
     * interceptors have a single seam to wrap.
     */
    @Bean("aiRestClient")
    public RestClient aiRestClient(AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        factory.setConnectTimeout((int) timeout.toMillis());
        factory.setReadTimeout((int) timeout.toMillis());
        return RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Phase 3 SSE streaming runs through a {@link WebClient} (Reactor) — we
     * use the JDK {@link HttpClient} connector to avoid pulling in
     * reactor-netty just for one feature. Connect timeout mirrors the
     * RestClient; read timeout is enforced per-subscription in the streaming
     * code path so partial chunks during a long generation don't trip an
     * idle-read timeout.
     */
    @Bean("aiWebClient")
    public WebClient aiWebClient(AiProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        return WebClient.builder()
                .clientConnector(new JdkClientHttpConnector(jdkClient))
                // Increase max in-memory size so SSE accumulators don't trip on
                // long streaming responses. 16 MiB is far beyond any chat turn.
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    /**
     * Placeholder customiser kept for backwards compatibility with anything
     * that already wires it. Streaming code uses {@link #aiWebClient} above.
     */
    @Bean
    public RestClientCustomizer aiRestClientCustomizer() {
        return builder -> { /* no-op */ };
    }
}
