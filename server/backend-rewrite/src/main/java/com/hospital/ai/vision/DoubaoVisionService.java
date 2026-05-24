package com.hospital.ai.vision;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.hospital.ai.audit.CurrentUserResolver;
import com.hospital.ai.client.AiCallTemplate;
import com.hospital.ai.client.ChatMessage;
import com.hospital.ai.client.ChatRequest;
import com.hospital.ai.client.ChatResponse;
import com.hospital.ai.config.AiProperties;
import com.hospital.photo.domain.Photo;
import com.hospital.photo.repository.PhotoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Doubao (Volcengine Ark) implementation of {@link AiVisionService}.
 *
 * <p>Flow per call:
 * <ol>
 *   <li>Resolve image bytes — either fetched from the {@code photo} table by
 *       id, or taken inline from the request.</li>
 *   <li>Build an OpenAI-compatible multimodal message: system prompt + user
 *       message carrying the image as a {@code data:} URL and a short text
 *       instruction.</li>
 *   <li>Delegate to {@link AiCallTemplate#chat(ChatRequest)} — that's where
 *       audit + rate-limit interceptors latch on. Temperature is forced to 1
 *       inside the template; we never pass it.</li>
 *   <li>Hand the response string to {@link MedicalRecordExtractor} to coerce
 *       into structured fields with a confidence heuristic.</li>
 *   <li>Persist exactly one {@link AiExtractionHistory} row regardless of
 *       success/failure so an admin can later debug.</li>
 * </ol>
 */
@Service
@ConditionalOnProperty(prefix = "hospital.ai", name = "enabled", havingValue = "true")
public class DoubaoVisionService implements AiVisionService {

    private static final Logger log = LoggerFactory.getLogger(DoubaoVisionService.class);
    private static final String PROMPT_PATH = "prompts/medical_record_extract.txt";
    private static final String FEATURE = "vision";
    /** Conservative pre-call estimate; multimodal image tokens dominate. */
    private static final int ESTIMATED_TOKENS = 2000;

    private final AiCallTemplate aiCallTemplate;
    private final AiProperties properties;
    private final PhotoRepository photoRepository;
    private final MedicalRecordExtractor extractor;
    private final AiExtractionHistoryRepository historyRepository;
    private final CurrentUserResolver currentUserResolver;
    private final String systemPrompt;

    public DoubaoVisionService(AiCallTemplate aiCallTemplate,
                               AiProperties properties,
                               PhotoRepository photoRepository,
                               MedicalRecordExtractor extractor,
                               AiExtractionHistoryRepository historyRepository,
                               CurrentUserResolver currentUserResolver) {
        this.aiCallTemplate = aiCallTemplate;
        this.properties = properties;
        this.photoRepository = photoRepository;
        this.extractor = extractor;
        this.historyRepository = historyRepository;
        this.currentUserResolver = currentUserResolver;
        this.systemPrompt = loadPrompt();
    }

    @Override
    @Transactional
    public AiVisionResult extractMedicalRecord(VisionExtractRequest request) {
        if (request == null || (!request.hasPhotoId() && !request.hasInlineData())) {
            throw new IllegalArgumentException("photoId 与 inline base64 二选一必须提供");
        }
        Long operatorId = currentUserResolver.currentUserId();
        UUID photoId = request.photoId();
        String dataUrl;
        try {
            dataUrl = resolveDataUrl(request);
        } catch (RuntimeException ex) {
            persistFailure(operatorId, photoId, ex, 0L, 0, 0);
            throw ex;
        }

        String model = properties.getVisionModel();
        String userInstruction = Optional.ofNullable(request.prompt())
                .filter(s -> !s.isBlank())
                .orElse("请按系统提示中的 JSON 结构提取这张病历图片的字段。");

        ChatRequest chatRequest = ChatRequest.builder()
                .feature(FEATURE)
                .model(model)
                .estimatedTokens(ESTIMATED_TOKENS)
                .messages(List.of(
                        ChatMessage.text("system", systemPrompt),
                        ChatMessage.multimodal("user", List.of(
                                ChatMessage.ContentPart.imageUrl(dataUrl),
                                ChatMessage.ContentPart.text(userInstruction)
                        ))
                ))
                .build();

        ChatResponse response;
        try {
            response = aiCallTemplate.chat(chatRequest);
        } catch (RuntimeException ex) {
            // AiRateLimitException and upstream errors land here. Audit /
            // rate-limit interceptor already logged a row in ai_audit_log; we
            // mirror to ai_extraction_history so vision-specific dashboards
            // also see the failure.
            persistFailure(operatorId, photoId, ex, 0L, 0, 0);
            throw ex;
        }

        MedicalRecordExtractor.Parsed parsed = extractor.parse(response.content());
        BigDecimal confidence = extractor.confidence(parsed.fields());
        String status = extractor.status(parsed.fields(), confidence);

        AiExtractionHistory row = new AiExtractionHistory();
        row.setSourceType(AiExtractionHistory.SOURCE_MEDICAL_RECORD);
        row.setSourceId(null); // record may not exist yet at extraction time
        row.setPhotoId(photoId);
        row.setOperatorId(operatorId);
        row.setModel(response.model() == null ? model : response.model());
        row.setRawJson(parsed.rawJson());
        row.setConfidence(confidence);
        row.setTokensIn(response.tokensIn());
        row.setTokensOut(response.tokensOut());
        row.setLatencyMs((int) Math.min(Integer.MAX_VALUE, response.latencyMs()));
        row.setStatus(status);

        AiExtractionHistory saved = historyRepository.save(row);

        AiVisionResult result = new AiVisionResult(
                parsed.rawJson(),
                parsed.fields(),
                confidence.doubleValue(),
                response.tokensIn(),
                response.tokensOut(),
                response.latencyMs(),
                saved.getId()
        );
        log.info("Vision extraction completed: model={} confidence={} tokensIn={} tokensOut={} latencyMs={}",
                model, confidence, response.tokensIn(), response.tokensOut(), response.latencyMs());
        return result;
    }

    // --- helpers -----------------------------------------------------------

    private String resolveDataUrl(VisionExtractRequest request) {
        if (request.hasPhotoId()) {
            Photo photo = photoRepository.findById(request.photoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "图片不存在：photoId=" + request.photoId()));
            String base64 = Base64.getEncoder().encodeToString(photo.getData());
            return "data:" + photo.getContentType() + ";base64," + base64;
        }
        // inline branch
        return "data:" + request.contentType() + ";base64," + request.base64();
    }

    private void persistFailure(Long operatorId, UUID photoId, Throwable error, long latency, int tIn, int tOut) {
        try {
            AiExtractionHistory row = new AiExtractionHistory();
            row.setSourceType(AiExtractionHistory.SOURCE_MEDICAL_RECORD);
            row.setPhotoId(photoId);
            row.setOperatorId(operatorId);
            row.setModel(properties.getVisionModel());
            row.setRawJson(Map.of("error", String.valueOf(error.getMessage())));
            row.setConfidence(BigDecimal.ZERO);
            row.setTokensIn(tIn);
            row.setTokensOut(tOut);
            row.setLatencyMs((int) Math.min(Integer.MAX_VALUE, latency));
            row.setStatus(AiExtractionHistory.STATUS_FAILED);
            row.setErrorMsg(truncate(String.valueOf(error.getMessage()), 1000));
            historyRepository.save(row);
        } catch (RuntimeException persistErr) {
            log.warn("Failed to persist ai_extraction_history failure row: {}", persistErr.toString());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String loadPrompt() {
        try {
            byte[] bytes = new ClassPathResource(PROMPT_PATH).getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load prompt template " + PROMPT_PATH, e);
        }
    }
}
