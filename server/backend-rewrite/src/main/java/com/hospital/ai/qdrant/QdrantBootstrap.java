package com.hospital.ai.qdrant;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ensures the configured Qdrant collection exists at boot. Idempotent — if the
 * collection is already present we leave it alone (even if the on-disk vector
 * size differs from {@link QdrantProperties#getVectorSize()}; a dimension
 * change is intentional and requires manual collection drop, otherwise old
 * vectors would be silently broken).
 *
 * <p>Runs at boot via {@link ApplicationRunner} so it is wired into the
 * normal startup phase and surfaces failures the same way as any other
 * bean.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class QdrantBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QdrantBootstrap.class);
    private static final long BOOT_TIMEOUT_SECONDS = 10L;

    private final QdrantClient client;
    private final QdrantProperties props;

    public QdrantBootstrap(QdrantClient client, QdrantProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureCollection();
        } catch (RuntimeException ex) {
            // Do NOT fail the whole boot — patient RAG is one feature, and
            // a Qdrant outage shouldn't prevent the rest of the system from
            // serving traffic. The /api/v1/ai/patient/*/ask endpoints will
            // raise 5xx at call time until Qdrant is reachable.
            log.warn("Qdrant bootstrap failed; patient RAG ingestion + search will fail until resolved: {}",
                    ex.toString());
        }
    }

    void ensureCollection() {
        String name = props.getCollection();
        boolean exists;
        try {
            exists = client.collectionExistsAsync(name)
                    .get(BOOT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while checking Qdrant collection", ie);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Failed to query Qdrant for collection '" + name + "'", ex);
        }
        if (exists) {
            log.info("Qdrant collection '{}' already exists; leaving config untouched", name);
            return;
        }
        Distance distance = parseDistance(props.getDistance());
        VectorParams params = VectorParams.newBuilder()
                .setSize(props.getVectorSize())
                .setDistance(distance)
                .build();
        try {
            client.createCollectionAsync(name, params)
                    .get(BOOT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Created Qdrant collection '{}' (size={}, distance={})",
                    name, props.getVectorSize(), distance);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating Qdrant collection", ie);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Failed to create Qdrant collection '" + name + "'", ex);
        }
    }

    static Distance parseDistance(String s) {
        if (s == null) return Distance.Cosine;
        return switch (s.trim().toLowerCase()) {
            case "cosine" -> Distance.Cosine;
            case "dot" -> Distance.Dot;
            case "euclid", "euclidean" -> Distance.Euclid;
            case "manhattan" -> Distance.Manhattan;
            default -> Distance.Cosine;
        };
    }
}
