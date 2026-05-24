package com.hospital.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code hospital.ai.*} block in application.yml.
 *
 * <p>The {@code enabled} flag is the master switch — when {@code false}, the
 * {@link AiConfig} declines to register any AI beans so an environment without
 * an API key can still boot cleanly.</p>
 */
@ConfigurationProperties(prefix = "hospital.ai")
public class AiProperties {

    private boolean enabled = false;
    private String provider = "doubao";
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";
    private String apiKey = "";
    private String chatModel = "doubao-seed-2.0-lite";
    private String embeddingModel = "doubao-embedding-text-240715";
    private String visionModel = "doubao-seed-2.0-lite";
    private int timeoutSeconds = 60;

    private final Features features = new Features();
    private final RateLimit rateLimit = new RateLimit();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getChatModel() { return chatModel; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public String getVisionModel() { return visionModel; }
    public void setVisionModel(String visionModel) { this.visionModel = visionModel; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public Features getFeatures() { return features; }
    public RateLimit getRateLimit() { return rateLimit; }

    public static class Features {
        private boolean vision = true;
        private boolean patientRag = true;
        private boolean consult = true;

        public boolean isVision() { return vision; }
        public void setVision(boolean vision) { this.vision = vision; }

        public boolean isPatientRag() { return patientRag; }
        public void setPatientRag(boolean patientRag) { this.patientRag = patientRag; }

        public boolean isConsult() { return consult; }
        public void setConsult(boolean consult) { this.consult = consult; }
    }

    public static class RateLimit {
        /** Requests per minute, per user. */
        private int perUserQpm = 10;
        /** Daily token budget, per user (input + output tokens combined). */
        private long dailyTokenBudget = 100_000L;

        public int getPerUserQpm() { return perUserQpm; }
        public void setPerUserQpm(int perUserQpm) { this.perUserQpm = perUserQpm; }

        public long getDailyTokenBudget() { return dailyTokenBudget; }
        public void setDailyTokenBudget(long dailyTokenBudget) { this.dailyTokenBudget = dailyTokenBudget; }
    }
}
