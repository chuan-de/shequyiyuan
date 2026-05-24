package com.hospital.ai.qdrant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code hospital.qdrant.*} block in
 * application.yml.
 *
 * <p>{@code vectorSize} MUST equal {@code hospital.ai.embedding-dimension}
 * (default 2048 for {@code doubao-embedding-vision-250615}). Mismatched
 * dimensions cause Qdrant to reject upsert with a clear error at boot — the
 * {@link QdrantBootstrap} runner re-creates the collection only when it does
 * not exist, so a dimension change requires manual collection deletion.</p>
 */
@ConfigurationProperties(prefix = "hospital.qdrant")
public class QdrantProperties {

    private String host = "localhost";
    /** gRPC port. The REST port (6333) is not used by the SDK. */
    private int port = 6334;
    private boolean useTls = false;
    /** Empty for local dev; required in production. */
    private String apiKey = "";
    private String collection = "patient_knowledge";
    private int vectorSize = 2048;
    /** Qdrant distance metric — Cosine | Euclid | Dot. */
    private String distance = "Cosine";

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }

    public int getVectorSize() { return vectorSize; }
    public void setVectorSize(int vectorSize) { this.vectorSize = vectorSize; }

    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
}
