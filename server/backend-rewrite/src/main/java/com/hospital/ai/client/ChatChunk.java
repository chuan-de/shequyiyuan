package com.hospital.ai.client;

/**
 * Single SSE delta from a streaming chat call. Phase 3 consumes these via
 * {@code AiCallTemplate#chatStream}.
 *
 * <p>The final {@code [DONE]} sentinel from the upstream is mapped to a chunk
 * with {@code done=true} and empty {@code deltaContent} so consumers can
 * unconditionally append {@code deltaContent} during the stream and check
 * {@code done} as a terminator. {@code tokensIn} / {@code tokensOut} are
 * populated only when the upstream included a {@code usage} block in the last
 * non-terminating chunk; otherwise they remain {@code null} and the auditor
 * falls back to a length-based estimate.</p>
 */
public record ChatChunk(String deltaContent, boolean done, Integer tokensIn, Integer tokensOut) {

    public static ChatChunk delta(String content) {
        return new ChatChunk(content == null ? "" : content, false, null, null);
    }

    public static ChatChunk terminator(Integer tokensIn, Integer tokensOut) {
        return new ChatChunk("", true, tokensIn, tokensOut);
    }
}

