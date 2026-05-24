package com.hospital.ai.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

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
     * Phase 3 will likely need a {@code WebClient} for SSE streaming. We
     * expose a builder customizer here as a placeholder so streaming code can
     * add it later without changing this configuration.
     */
    @Bean
    public RestClientCustomizer aiRestClientCustomizer() {
        return builder -> {
            // No-op for Phase 0; left as the wiring seam for streaming.
        };
    }
}
