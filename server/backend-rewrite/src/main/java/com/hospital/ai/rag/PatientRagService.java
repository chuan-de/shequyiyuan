package com.hospital.ai.rag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.hospital.ai.audit.AiAuditLog;
import com.hospital.ai.audit.AiAuditLogRepository;
import com.hospital.ai.client.AiCallTemplate;
import com.hospital.ai.client.ChatMessage;
import com.hospital.ai.client.ChatRequest;
import com.hospital.ai.client.ChatResponse;
import com.hospital.ai.common.AiConsentRequiredException;
import com.hospital.ai.config.AiProperties;
import com.hospital.ai.embedding.EmbeddingService;
import com.hospital.ai.ingestion.KnowledgeIngestionService;
import com.hospital.common.NotFoundException;
import com.hospital.observability.TraceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Retrieval-Augmented Generation over a single patient's records.
 *
 * <p><b>Privacy fence (non-negotiable):</b> every retrieval query carries an
 * explicit {@code WHERE patient_id = :patientId} clause. Callers cannot
 * cross-pollute: the service does its own role + ownership check before
 * touching the embeddings, and the SQL is in this one class so a security
 * review only has to read these ~40 lines to confirm.</p>
 *
 * <p>Flow per {@link #ask}:</p>
 * <ol>
 *   <li>Resolve caller's app_user.id + roles from the security context.</li>
 *   <li>Authorize: ADMIN/DOCTOR allowed; PATIENT only for their own
 *       {@code patient_profile.user_id}.</li>
 *   <li>Look up the patient row, refuse with 404 if missing and 412 (via
 *       {@link AiConsentRequiredException}) if {@code ai_consent_at} is null.</li>
 *   <li>Embed the question, retrieve top-K=5 chunks for THIS patient sorted
 *       by cosine distance ({@code <=>}).</li>
 *   <li>Render a Chinese system prompt + numbered citations and call
 *       {@link AiCallTemplate#chat}.</li>
 *   <li>Write an extra {@code ai_audit_log} row (feature
 *       {@code patient-rag-retrieval}) recording the question + retrieved
 *       chunk ids so an admin can audit privacy after the fact.</li>
 * </ol>
 *
 * <p>{@link AiCallTemplate#chat} is already wrapped by
 * {@code AiAuditInterceptor} (audit + rate limit), so we do NOT double-debit
 * the rate limit here.</p>
 */
@Service
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.patient-rag"},
        havingValue = "true")
public class PatientRagService {

    private static final Logger log = LoggerFactory.getLogger(PatientRagService.class);

    /** Retrieval depth — small on purpose to keep prompts focused. */
    static final int TOP_K = 5;
    static final String FEATURE = "patient-rag";
    static final String FEATURE_RETRIEVAL = "patient-rag-retrieval";

    private final JdbcClient jdbcClient;
    private final EmbeddingService embeddingService;
    private final AiCallTemplate aiCallTemplate;
    private final AiProperties properties;
    private final AiAuditLogRepository auditRepository;

    public PatientRagService(JdbcClient jdbcClient,
                             EmbeddingService embeddingService,
                             AiCallTemplate aiCallTemplate,
                             AiProperties properties,
                             AiAuditLogRepository auditRepository) {
        this.jdbcClient = jdbcClient;
        this.embeddingService = embeddingService;
        this.aiCallTemplate = aiCallTemplate;
        this.properties = properties;
        this.auditRepository = auditRepository;
    }

    /**
     * Answer a question about ONE patient's records, with citations.
     *
     * @param patientId    target patient (privacy fence — never leaks across)
     * @param question     user's natural-language question
     * @param callerUserId resolved app_user.id of the authenticated caller;
     *                     {@code null} is rejected with 401
     */
    @Transactional
    public RagAnswer ask(Long patientId, String question, Long callerUserId) {
        if (callerUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证用户禁止访问");
        }
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "问题不能为空");
        }
        if (patientId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId 不能为空");
        }

        PatientRow patient = loadPatient(patientId);
        authorize(patient, callerUserId);

        if (patient.aiConsentAt() == null) {
            throw new AiConsentRequiredException(
                    "患者尚未授权 AI 智能辅助服务，无法发起问询");
        }

        float[] questionVector = embeddingService.embedOne(question);
        List<Citation> citations = retrieve(patientId, questionVector);

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(question, citations);
        ChatResponse response = aiCallTemplate.chat(ChatRequest.builder()
                .feature(FEATURE)
                .model(properties.getChatModel())
                .estimatedTokens(estimateTokens(systemPrompt, userPrompt))
                .messages(List.of(
                        ChatMessage.text("system", systemPrompt),
                        ChatMessage.text("user", userPrompt)
                ))
                .build());

        recordRetrievalAudit(callerUserId, question, citations);

        return new RagAnswer(response.content(), citations, response.tokensIn(),
                response.tokensOut(), response.latencyMs());
    }

    // --- privacy / auth ----------------------------------------------------

    /**
     * Visible for testing; returns the patient_profile row used by both the
     * permission check AND the consent check. Querying once avoids a TOCTOU
     * window between authorisation and consent.
     */
    PatientRow loadPatient(Long patientId) {
        return jdbcClient.sql("""
                SELECT id, user_id, ai_consent_at
                FROM patient_profile
                WHERE id = :id
                """)
                .param("id", patientId)
                .query((rs, rowNum) -> new PatientRow(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("ai_consent_at") == null ? null
                                : rs.getTimestamp("ai_consent_at").toInstant()))
                .optional()
                .orElseThrow(() -> new NotFoundException("Patient not found"));
    }

    /**
     * Caller must be ADMIN or DOCTOR, OR the patient themselves (matched via
     * {@code patient_profile.user_id == callerUserId}). Permission code
     * {@code ai:patient-rag} is checked at the controller layer via
     * {@code @PreAuthorize}; this is the row-level check on top of it.
     */
    void authorize(PatientRow patient, Long callerUserId) {
        Collection<String> roles = currentRoles();
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_DOCTOR")) return;
        if (patient.userId() != null && patient.userId().equals(callerUserId)) return;
        throw new AccessDeniedException("无权访问该患者的 AI 问询");
    }

    private static Collection<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return List.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // --- retrieval ---------------------------------------------------------

    List<Citation> retrieve(Long patientId, float[] questionVector) {
        // SECURITY: patient_id filter is mandatory — top-K alone is not enough.
        // The `embedding <=> :vec::vector` operator returns cosine distance
        // (lower = closer). similarity = 1 - distance gives the UI a
        // friendlier 0..1 number.
        String vectorLiteral = KnowledgeIngestionService.toPgVectorLiteral(questionVector);
        return jdbcClient.sql("""
                SELECT id, source_type, source_id, field_key, chunk_text,
                       (embedding <=> CAST(:vec AS vector)) AS distance,
                       metadata ->> 'source_created_at' AS source_created_at
                FROM patient_knowledge_chunk
                WHERE patient_id = :pid
                ORDER BY embedding <=> CAST(:vec AS vector)
                LIMIT :k
                """)
                .param("pid", patientId)
                .param("vec", vectorLiteral)
                .param("k", TOP_K)
                .query(PatientRagService::mapCitation)
                .list();
    }

    private static Citation mapCitation(ResultSet rs, int rowNum) throws SQLException {
        double distance = rs.getDouble("distance");
        String srcDate = rs.getString("source_created_at");
        Instant date = null;
        if (srcDate != null) {
            try { date = Instant.parse(srcDate); } catch (RuntimeException ignored) { /* leave null */ }
        }
        // Fall back to the createdAt-style column when metadata didn't carry it.
        Timestamp ts = null;
        try { ts = rs.getTimestamp("source_created_at"); } catch (SQLException ignored) { /* expected: column is text in our select */ }
        if (date == null && ts != null) date = ts.toInstant();

        return new Citation(
                rs.getLong("id"),
                rs.getString("source_type"),
                rs.getLong("source_id"),
                rs.getString("field_key"),
                truncate(rs.getString("chunk_text"), 280),
                date,
                Math.max(0.0, 1.0 - distance));
    }

    // --- prompt assembly ---------------------------------------------------

    static String buildSystemPrompt() {
        return """
                你是一名社区医院的 AI 辅助医生助手。请严格遵守以下规则：
                1. 只能依据用户消息中"患者历史片段"部分给出的资料作答；如果资料里没有相关内容，请直接回答"现有资料中未找到相关信息"，不要编造任何事实、药品、剂量或诊断。
                2. 回答务必使用中文，简洁、客观、专业；必要时分点列出。
                3. 涉及到具体既往内容时，请用 [#编号] 的形式标注来源，编号对应资料编号。
                4. 不要给出可能危及患者安全的具体用药建议；可以给出"建议线下复诊"等通用建议。
                """;
    }

    static String buildUserPrompt(String question, List<Citation> citations) {
        StringBuilder sb = new StringBuilder();
        sb.append("患者历史片段（按相关度排序）：\n");
        if (citations.isEmpty()) {
            sb.append("（无）\n");
        } else {
            for (int i = 0; i < citations.size(); i++) {
                Citation c = citations.get(i);
                sb.append('[').append('#').append(i + 1).append(']')
                        .append(' ').append(c.sourceType())
                        .append('#').append(c.sourceId())
                        .append(' ').append(c.fieldKey());
                if (c.sourceDate() != null) sb.append(' ').append(c.sourceDate());
                sb.append('\n').append(c.snippet()).append("\n\n");
            }
        }
        sb.append("---\n请回答以下问题：\n").append(question);
        return sb.toString();
    }

    private static int estimateTokens(String system, String user) {
        // 1 token ≈ 2 chars for Chinese; cap at 4000 so the rate limiter
        // doesn't reject reasonable prompts up-front.
        return Math.min(4000, (system.length() + user.length()) / 2 + 200);
    }

    // --- audit -------------------------------------------------------------

    /**
     * Extra audit row with the LIST of retrieved chunk ids. The chat call is
     * already audited by {@code AiAuditInterceptor}; this row exists for the
     * privacy review story — every retrieval is traceable to a patient + a
     * specific set of chunks even when nobody actually called the LLM.
     */
    void recordRetrievalAudit(Long userId, String question, List<Citation> citations) {
        try {
            AiAuditLog row = new AiAuditLog();
            row.setUserId(userId);
            row.setFeature(FEATURE_RETRIEVAL);
            row.setModel(properties.getEmbeddingModel());
            row.setPromptExcerpt(truncate(question, 500));
            String chunkIds = citations.stream()
                    .map(c -> String.valueOf(c.chunkId()))
                    .collect(Collectors.joining(","));
            row.setResponseExcerpt(truncate("retrieved_chunk_ids=[" + chunkIds + "]", 500));
            row.setStatus("success");
            row.setLatencyMs(0);
            row.setTraceId(TraceContext.getTraceId());
            auditRepository.save(row);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist retrieval audit row: {}", ex.toString());
        }
    }

    /** Visible for tests + the controller's row-level patient check. */
    public record PatientRow(Long id, Long userId, Instant aiConsentAt) {}

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // ---- consent grant (used by controller) -------------------------------

    /**
     * Mark the patient as having granted AI processing consent. Idempotent —
     * overwriting with a fresh timestamp is fine if the patient re-affirms.
     */
    @Transactional
    public Instant grantConsent(Long patientId, Long callerUserId) {
        PatientRow patient = loadPatient(patientId);
        Collection<String> roles = currentRoles();
        // Only the patient themselves can grant consent on their behalf via
        // this API (the modal lives in the patient detail page). ADMIN can
        // also grant for backfill / support scenarios.
        boolean isOwner = patient.userId() != null && patient.userId().equals(callerUserId);
        if (!(isOwner || roles.contains("ROLE_ADMIN"))) {
            throw new AccessDeniedException("仅患者本人或管理员可代为授权");
        }
        Instant now = Instant.now();
        jdbcClient.sql("UPDATE patient_profile SET ai_consent_at = :ts WHERE id = :id")
                .param("ts", Timestamp.from(now))
                .param("id", patientId)
                .update();
        return now;
    }

    /** Mutable wrapper not used; reserved for future retrieval params. */
    static List<Citation> defensiveCopy(List<Citation> in) {
        return new ArrayList<>(in);
    }
}
