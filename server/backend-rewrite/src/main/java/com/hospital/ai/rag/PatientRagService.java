package com.hospital.ai.rag;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
import com.hospital.ai.qdrant.PatientKnowledgeStore;
import com.hospital.ai.qdrant.RetrievedChunk;
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
 * <p><b>Privacy fence (non-negotiable):</b> every retrieval query goes through
 * {@link PatientKnowledgeStore#search} which applies a mandatory
 * {@code match(patient_id, ?)} filter — there is no overload that bypasses it.
 * Callers cannot cross-pollute: the service does its own role + ownership
 * check before touching the embeddings.</p>
 *
 * <p>Flow per {@link #ask}:</p>
 * <ol>
 *   <li>Resolve caller's app_user.id + roles from the security context.</li>
 *   <li>Authorize: ADMIN/DOCTOR allowed; PATIENT only for their own
 *       {@code patient_profile.user_id}.</li>
 *   <li>Look up the patient row, refuse with 404 if missing and 412 (via
 *       {@link AiConsentRequiredException}) if {@code ai_consent_at} is null.</li>
 *   <li>Embed the question, retrieve top-K=5 chunks for THIS patient from
 *       Qdrant (Cosine similarity).</li>
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
    /** Marker the model emits when it actually cites a retrieved chunk. */
    private static final Pattern CITATION_MARKER = Pattern.compile("\\[#\\d+\\]");

    private final JdbcClient jdbcClient;
    private final EmbeddingService embeddingService;
    private final PatientKnowledgeStore knowledgeStore;
    private final AiCallTemplate aiCallTemplate;
    private final AiProperties properties;
    private final AiAuditLogRepository auditRepository;

    public PatientRagService(JdbcClient jdbcClient,
                             EmbeddingService embeddingService,
                             PatientKnowledgeStore knowledgeStore,
                             AiCallTemplate aiCallTemplate,
                             AiProperties properties,
                             AiAuditLogRepository auditRepository) {
        this.jdbcClient = jdbcClient;
        this.embeddingService = embeddingService;
        this.knowledgeStore = knowledgeStore;
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
        String userPrompt = buildUserPrompt(question, citations, buildPatientContextBlock(patientId));
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

        // 模型一个 [#编号] 都没引用（基础档案足以作答，或资料与问题无关）时
        // 不向前端返回引用列表，避免"未找到相关信息"还挂着一排来源。
        String content = response.content();
        List<Citation> citedOnly = (content != null && CITATION_MARKER.matcher(content).find())
                ? citations : List.of();

        return new RagAnswer(content, citedOnly, response.tokensIn(),
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

    /**
     * SECURITY: the {@code patientId} filter is enforced inside
     * {@link PatientKnowledgeStore#search}; cross-patient leakage is
     * impossible at this layer without modifying that one method.
     */
    List<Citation> retrieve(Long patientId, float[] questionVector) {
        List<RetrievedChunk> hits = knowledgeStore.search(patientId, questionVector, TOP_K);
        List<Citation> out = new ArrayList<>(hits.size());
        // Synthetic chunk id surfaced to the UI — Qdrant's UUID point ids would
        // be awkward to display. The retrieval audit log records the index
        // alongside the (source_type, source_id, field_key) tuple so a privacy
        // audit can still trace exact provenance.
        for (int i = 0; i < hits.size(); i++) {
            RetrievedChunk h = hits.get(i);
            Map<String, Object> meta = h.metadata();
            Instant date = parseInstant(meta == null ? null : meta.get("source_created_at"));
            // Qdrant returns Cosine similarity in [-1, 1] (typically [0, 1] for
            // normalised embeddings). Clamp negatives to 0 so the UI never
            // shows a nonsense bar.
            double similarity = Math.max(0.0, Math.min(1.0, h.score()));
            out.add(new Citation(
                    (long) (i + 1),
                    h.sourceType(),
                    h.sourceId(),
                    h.fieldKey(),
                    truncate(h.chunkText(), 280),
                    date,
                    similarity));
        }
        return out;
    }

    private static Instant parseInstant(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Instant inst) return inst;
        String s = raw.toString();
        if (s.isBlank()) return null;
        try { return Instant.parse(s); } catch (RuntimeException ignored) { return null; }
    }

    // --- prompt assembly ---------------------------------------------------

    static String buildSystemPrompt() {
        return """
                你是一名社区医院的 AI 辅助医生助手。请严格遵守以下规则：
                1. 只能依据用户消息中"患者基础档案"（档案信息、近期随访指标、家庭医生签约）与"患者历史片段"（病历/就诊检索结果）两部分资料作答；如果资料里没有相关内容，请直接回答"现有资料中未找到相关信息"，不要编造任何事实、药品、剂量或诊断。
                2. 回答务必使用中文，简洁、客观、专业；必要时分点列出。
                3. 引用"患者历史片段"中的内容时，请用 [#编号] 的形式标注来源，编号对应片段编号；引用"患者基础档案"无需标注。若历史片段与问题无关，请不要输出任何 [#编号]。
                4. 不要给出可能危及患者安全的具体用药建议；可以给出"建议线下复诊"等通用建议。
                """;
    }

    /**
     * 患者 360° 基础上下文：档案要点 + 近 5 次随访指标 + 生效签约。
     * 这类信息小而常驻、随档案实时变化，直接注入 prompt 比向量化更可靠
     * （无需重嵌入即可保证最新，也不会被相似度检索漏掉）。
     */
    String buildPatientContextBlock(Long patientId) {
        StringBuilder sb = new StringBuilder();
        jdbcClient.sql("""
                SELECT full_name, sex_types, birth_date, allergies, medical_history, address
                FROM patient_profile WHERE id = :id
                """)
                .param("id", patientId)
                .query((rs, n) -> {
                    sb.append("姓名：").append(nullable(rs.getString("full_name"))).append('\n');
                    Integer sex = rs.getObject("sex_types") != null ? rs.getInt("sex_types") : null;
                    sb.append("性别：").append(sex == null ? "未知" : (sex == 1 ? "男" : sex == 2 ? "女" : String.valueOf(sex))).append('\n');
                    java.sql.Date bd = rs.getDate("birth_date");
                    if (bd != null) {
                        LocalDate birth = bd.toLocalDate();
                        sb.append("出生日期：").append(birth)
                          .append("（").append(Period.between(birth, LocalDate.now()).getYears()).append(" 岁）\n");
                    }
                    appendIfPresent(sb, "过敏史", rs.getString("allergies"));
                    appendIfPresent(sb, "既往病史", rs.getString("medical_history"));
                    appendIfPresent(sb, "住址", rs.getString("address"));
                    return null;
                })
                .optional();

        jdbcClient.sql("""
                SELECT fd.full_name AS doctor_name, c.service_package, c.signed_at, c.expires_at
                FROM family_doctor_contract c
                JOIN family_doctor_profile fd ON fd.id = c.family_doctor_id
                WHERE c.patient_id = :id AND c.status = 'ACTIVE'
                  AND (c.expires_at IS NULL OR c.expires_at >= CURRENT_DATE)
                ORDER BY c.signed_at DESC LIMIT 1
                """)
                .param("id", patientId)
                .query((rs, n) -> {
                    sb.append("家庭医生签约：").append(nullable(rs.getString("doctor_name")));
                    if (rs.getString("service_package") != null) sb.append("（").append(rs.getString("service_package")).append("）");
                    if (rs.getDate("signed_at") != null) sb.append("，签约日期 ").append(rs.getDate("signed_at"));
                    sb.append('\n');
                    return null;
                })
                .optional();

        List<String> followups = jdbcClient.sql("""
                SELECT measured_at, systolic, diastolic, blood_sugar, height_cm, weight_kg, heart_rate, notes
                FROM patient_followup WHERE patient_id = :id
                ORDER BY measured_at DESC LIMIT 5
                """)
                .param("id", patientId)
                .query((rs, n) -> {
                    StringBuilder f = new StringBuilder();
                    f.append("- ").append(rs.getTimestamp("measured_at").toInstant().toString(), 0, 10);
                    if (rs.getObject("systolic") != null && rs.getObject("diastolic") != null) {
                        f.append(" 血压 ").append(rs.getInt("systolic")).append('/').append(rs.getInt("diastolic")).append(" mmHg");
                    }
                    if (rs.getBigDecimal("blood_sugar") != null) f.append(" 血糖 ").append(rs.getBigDecimal("blood_sugar")).append(" mmol/L");
                    if (rs.getBigDecimal("weight_kg") != null) f.append(" 体重 ").append(rs.getBigDecimal("weight_kg")).append(" kg");
                    if (rs.getObject("heart_rate") != null) f.append(" 心率 ").append(rs.getInt("heart_rate")).append(" bpm");
                    if (rs.getString("notes") != null && !rs.getString("notes").isBlank()) f.append("，备注：").append(rs.getString("notes"));
                    return f.toString();
                })
                .list();
        if (!followups.isEmpty()) {
            sb.append("近期随访指标（最新在前）：\n");
            followups.forEach(f -> sb.append(f).append('\n'));
        }
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) sb.append(label).append('：').append(value).append('\n');
    }

    private static String nullable(String s) { return s == null ? "未知" : s; }

    static String buildUserPrompt(String question, List<Citation> citations, String patientContextBlock) {
        StringBuilder sb = new StringBuilder();
        sb.append("患者基础档案：\n");
        if (patientContextBlock == null || patientContextBlock.isBlank()) {
            sb.append("（无）\n");
        } else {
            sb.append(patientContextBlock);
        }
        sb.append("\n患者历史片段（按相关度排序）：\n");
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
     * Extra audit row with the LIST of retrieved chunk references. The chat
     * call is already audited by {@code AiAuditInterceptor}; this row exists
     * for the privacy review story — every retrieval is traceable to a
     * patient + a specific set of chunks even when nobody actually called
     * the LLM.
     */
    void recordRetrievalAudit(Long userId, String question, List<Citation> citations) {
        try {
            AiAuditLog row = new AiAuditLog();
            row.setUserId(userId);
            row.setFeature(FEATURE_RETRIEVAL);
            row.setModel(properties.getEmbeddingModel());
            row.setPromptExcerpt(truncate(question, 500));
            String refs = citations.stream()
                    .map(c -> c.sourceType() + "#" + c.sourceId() + ":" + c.fieldKey())
                    .collect(Collectors.joining(","));
            row.setResponseExcerpt(truncate("retrieved=[" + refs + "]", 500));
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
