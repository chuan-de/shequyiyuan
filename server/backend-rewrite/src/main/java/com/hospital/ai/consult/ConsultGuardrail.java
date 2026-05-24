package com.hospital.ai.consult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Cheap, deterministic pre-flight check on incoming user messages. We
 * deliberately do NOT call the LLM to decide refusal — keyword lists are
 * predictable, auditable, and never burn token budget on garbage input.
 *
 * <p>Three layers:</p>
 * <ul>
 *   <li><b>Length</b>: under 2 chars or over 2000 chars is refused.</li>
 *   <li><b>Off-topic keywords</b> (weather, code, lyrics, …) — friendly
 *       redirect to the appropriate tool.</li>
 *   <li><b>Jailbreak keywords</b> (ignore previous, system prompt, 越狱, …)
 *       — short refusal asking for civilised input.</li>
 * </ul>
 *
 * <p>Keyword list lives in {@code classpath:guardrail/refused_keywords.txt}.
 * It is loaded once at startup; a hot-reload knob is left as a TODO for
 * Phase 4.</p>
 */
@Component
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.consult"},
        havingValue = "true")
public class ConsultGuardrail {

    private static final Logger log = LoggerFactory.getLogger(ConsultGuardrail.class);

    static final int MIN_LENGTH = 2;
    static final int MAX_LENGTH = 2000;

    static final String CATEGORY_OFF_TOPIC = "off_topic";
    static final String CATEGORY_JAILBREAK = "jailbreak";

    static final String MSG_TOO_SHORT = "请描述更详细一些，我才能给出建议哦～";
    static final String MSG_TOO_LONG = "提问内容过长，请精简到 2000 字以内。";
    static final String MSG_OFF_TOPIC = "我是健康助手，专注健康咨询，其他话题请使用相应工具～";
    static final String MSG_JAILBREAK = "请文明用语，专注健康话题。";

    private final List<KeywordRule> rules;

    public ConsultGuardrail() {
        this(loadDefault());
    }

    /** Visible for testing. */
    ConsultGuardrail(List<KeywordRule> rules) {
        this.rules = Collections.unmodifiableList(rules);
    }

    public GuardrailResult check(String userMessage) {
        if (userMessage == null) {
            return GuardrailResult.refused(MSG_TOO_SHORT);
        }
        String trimmed = userMessage.trim();
        if (trimmed.length() < MIN_LENGTH) {
            return GuardrailResult.refused(MSG_TOO_SHORT);
        }
        if (trimmed.length() > MAX_LENGTH) {
            return GuardrailResult.refused(MSG_TOO_LONG);
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (KeywordRule rule : rules) {
            if (lower.contains(rule.keyword())) {
                if (CATEGORY_JAILBREAK.equals(rule.category())) {
                    return GuardrailResult.refused(MSG_JAILBREAK);
                }
                return GuardrailResult.refused(MSG_OFF_TOPIC);
            }
        }
        return GuardrailResult.ok();
    }

    private static List<KeywordRule> loadDefault() {
        ClassPathResource resource = new ClassPathResource("guardrail/refused_keywords.txt");
        List<KeywordRule> out = new ArrayList<>();
        try (InputStream is = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int sep = trimmed.indexOf('|');
                if (sep <= 0 || sep == trimmed.length() - 1) {
                    log.warn("Skipping malformed guardrail rule: {}", trimmed);
                    continue;
                }
                String category = trimmed.substring(0, sep).trim();
                String keyword = trimmed.substring(sep + 1).trim().toLowerCase(Locale.ROOT);
                out.add(new KeywordRule(category, keyword));
            }
        } catch (IOException ex) {
            log.error("Failed to load guardrail keyword file; consult endpoint will allow all input until fixed", ex);
        }
        return out;
    }

    /** Visible for tests. */
    int ruleCount() { return rules.size(); }

    public record KeywordRule(String category, String keyword) {}

    public record GuardrailResult(boolean allowed, String refusalMessage) {
        public static GuardrailResult ok() { return new GuardrailResult(true, null); }
        public static GuardrailResult refused(String msg) { return new GuardrailResult(false, msg); }
    }
}
