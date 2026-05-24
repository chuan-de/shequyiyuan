package com.hospital.ai.rag;

import java.util.List;

/**
 * Answer + provenance returned by {@link PatientRagService#ask}.
 * Mirror of the wire format the frontend renders in the AI 问询 sidebar.
 */
public record RagAnswer(
        String answer,
        List<Citation> citations,
        int tokensIn,
        int tokensOut,
        long latencyMs) {
}
