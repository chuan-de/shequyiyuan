package com.hospital.ai.consult;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.hospital.ai.client.ChatMessage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Builds the OpenAI-compatible message list to send for one streaming turn.
 *
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Always include the canonical Chinese system primer at index 0 — even
 *       if the session never had one persisted (older sessions, future
 *       prompt tweaks). User-authored system messages are dropped so a user
 *       can't override the safety primer via prompt injection.</li>
 *   <li>Append history in chronological order.</li>
 *   <li>If estimated tokens exceed {@link #MAX_CONTEXT_TOKENS}, drop the
 *       OLDEST user/assistant turn (pair) one at a time until under budget.
 *       The system primer is never dropped.</li>
 * </ol>
 *
 * <p>Token estimation is intentionally crude (1 char ≈ 1 token) — we do not
 * pull in a tokenizer dependency for Phase 3. The 32K threshold is also
 * generous; if it ever bites in practice we'll swap for jtokkit.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.consult"},
        havingValue = "true")
public class ConsultContextBuilder {

    /** Soft budget — Doubao supports more, but we cap aggressively for cost. */
    static final int MAX_CONTEXT_TOKENS = 32_000;

    /** Canonical Chinese safety primer. Pinned here so reviews catch any tweak. */
    public static final String DEFAULT_SYSTEM_PROMPT =
            "你是社区医院的 AI 健康助手。请基于常识为用户提供初步健康建议，绝不替代专业诊疗。"
                    + "回答须客观、谨慎，鼓励就医。不回答与健康无关的问题。";

    /**
     * @param history all messages for the session, in insertion order
     * @return system + windowed history, ready for {@code AiCallTemplate}
     */
    public List<ChatMessage> build(List<AiConsultMessage> history) {
        // Drop any system-role rows from history; we always re-inject our own
        // primer at position 0 to defeat prompt-injection via stored history.
        Deque<AiConsultMessage> nonSystem = new LinkedList<>();
        for (AiConsultMessage m : history) {
            if (!AiConsultMessage.ROLE_SYSTEM.equalsIgnoreCase(m.getRole())) {
                nonSystem.add(m);
            }
        }

        int budget = MAX_CONTEXT_TOKENS - estimate(DEFAULT_SYSTEM_PROMPT);
        int used = 0;
        for (AiConsultMessage m : nonSystem) {
            used += estimate(m.getContent());
        }

        // Drop oldest pairs until inside budget. We drop in pairs (user +
        // assistant) when possible so the conversation reads coherently.
        while (used > budget && !nonSystem.isEmpty()) {
            AiConsultMessage first = nonSystem.pollFirst();
            used -= estimate(first.getContent());
            // If the dropped one was a user message, also drop its assistant
            // reply (peek next) to keep alternation.
            if (AiConsultMessage.ROLE_USER.equalsIgnoreCase(first.getRole())
                    && !nonSystem.isEmpty()
                    && AiConsultMessage.ROLE_ASSISTANT.equalsIgnoreCase(nonSystem.peekFirst().getRole())) {
                AiConsultMessage second = nonSystem.pollFirst();
                used -= estimate(second.getContent());
            }
        }

        List<ChatMessage> out = new ArrayList<>(nonSystem.size() + 1);
        out.add(ChatMessage.text("system", DEFAULT_SYSTEM_PROMPT));
        for (AiConsultMessage m : nonSystem) {
            out.add(ChatMessage.text(m.getRole(), m.getContent()));
        }
        return out;
    }

    /**
     * Crude per-content token estimate. Chinese 1:1 is acceptable for a soft
     * budget; pull in jtokkit if we ever need precision.
     */
    static int estimate(String content) {
        return content == null ? 0 : content.length();
    }
}
