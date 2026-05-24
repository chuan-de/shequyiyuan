package com.hospital.ai.client;

/**
 * Single SSE delta from a streaming chat call. Phase 3 will consume these via
 * {@code AiCallTemplate#chatStream}; for Phase 0 the record is reserved.
 */
public record ChatChunk(String deltaContent, boolean done) { }
