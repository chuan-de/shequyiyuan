package com.hospital.ai.qdrant;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the {@link QdrantClient} bean used by {@link PatientKnowledgeStore} for
 * Phase 2 patient RAG storage. Gated by the same master switches as the rest
 * of the AI stack — when {@code hospital.ai.enabled} or
 * {@code hospital.ai.features.patient-rag} is false, no Qdrant connection is
 * opened.
 */
@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class QdrantConfig {

    /**
     * gRPC-backed Qdrant client. The {@code destroyMethod} ensures the
     * underlying Netty channel is closed cleanly on container shutdown so
     * tests + dev reloads don't leak threads.
     */
    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient(QdrantProperties props) {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                props.getHost(), props.getPort(), props.isUseTls());
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            builder.withApiKey(props.getApiKey());
        }
        return new QdrantClient(builder.build());
    }
}
