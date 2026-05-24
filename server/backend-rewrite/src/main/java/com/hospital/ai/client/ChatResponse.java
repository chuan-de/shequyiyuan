package com.hospital.ai.client;

/**
 * Result of a synchronous chat call. {@code tokensIn} / {@code tokensOut} are
 * surfaced so the audit + rate-limit layers can debit the user's daily budget
 * without re-parsing the upstream response.
 */
public record ChatResponse(
        String model,
        String content,
        int tokensIn,
        int tokensOut,
        long latencyMs
) {
    public int totalTokens() { return tokensIn + tokensOut; }
}
