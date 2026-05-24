package com.hospital.ai.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic helpers for stripping the cosmetic wrappers Doubao / OpenAI-style
 * chat models add around JSON responses (markdown fences, prose). Lifted out
 * of {@code com.hospital.ai.vision.MedicalRecordExtractor} so Phase 2 (RAG)
 * and Phase 3 (consult) can reuse the same defensive parsing without
 * depending on the vision package.
 *
 * <p>The class is purely static — no Spring bean needed.</p>
 */
public final class JsonResponseParser {

    private static final Pattern JSON_FENCE =
            Pattern.compile("```(?:json)?\\s*(.*?)```", Pattern.DOTALL);
    private static final Pattern FIRST_JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");

    private JsonResponseParser() {}

    /**
     * Strip a single leading/trailing markdown JSON fence if present. Returns
     * the input untouched when no fence is found so callers can chain safely.
     */
    public static String stripMarkdownFence(String s) {
        if (s == null) return null;
        Matcher m = JSON_FENCE.matcher(s);
        if (m.find()) return m.group(1);
        return s;
    }

    /**
     * Best-effort: return the first {@code {...}} substring in the input so
     * callers can recover JSON even when the model wraps it in prose. Returns
     * {@code null} when no JSON object is found.
     */
    public static String extractFirstJsonObject(String s) {
        if (s == null) return null;
        Matcher m = FIRST_JSON_OBJECT.matcher(s);
        return m.find() ? m.group() : null;
    }
}
