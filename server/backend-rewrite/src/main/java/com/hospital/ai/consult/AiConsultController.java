package com.hospital.ai.consult;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.ai.audit.CurrentUserResolver;
import com.hospital.ai.client.AiCallTemplate;
import com.hospital.ai.client.ChatChunk;
import com.hospital.ai.client.ChatMessage;
import com.hospital.ai.client.ChatRequest;
import com.hospital.ai.common.AiRateLimitException;
import com.hospital.ai.config.AiProperties;
import com.hospital.ai.consult.ConsultGuardrail.GuardrailResult;
import com.hospital.common.ApiResponse;
import com.hospital.common.PageResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST + SSE surface for community AI consult.
 *
 * <p>All endpoints require permission {@code ai:consult} AND enforce row-level
 * isolation by {@code app_user.id} — even ADMIN cannot read another user's
 * sessions. The streaming endpoint is the only one that opens a long-lived
 * connection; everything else is a normal short JSON response.</p>
 *
 * <p>The whole controller is gated on
 * {@code hospital.ai.features.consult=true}; when off the bean is not
 * registered so the routes 404 instead of 403.</p>
 */
@RestController
@RequestMapping("/api/v1/ai/consult")
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.consult"},
        havingValue = "true")
public class AiConsultController {

    private static final Logger log = LoggerFactory.getLogger(AiConsultController.class);

    private final AiConsultService consultService;
    private final ConsultContextBuilder contextBuilder;
    private final ConsultGuardrail guardrail;
    private final AiCallTemplate aiCallTemplate;
    private final AiProperties properties;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiConsultController(AiConsultService consultService,
                               ConsultContextBuilder contextBuilder,
                               ConsultGuardrail guardrail,
                               AiCallTemplate aiCallTemplate,
                               AiProperties properties,
                               CurrentUserResolver currentUserResolver) {
        this.consultService = consultService;
        this.contextBuilder = contextBuilder;
        this.guardrail = guardrail;
        this.aiCallTemplate = aiCallTemplate;
        this.properties = properties;
        this.currentUserResolver = currentUserResolver;
    }

    // ---- session CRUD ------------------------------------------------------

    @PostMapping("/sessions")
    @PreAuthorize("hasAuthority('ai:consult')")
    public ApiResponse<SessionView> createSession(@RequestBody(required = false) CreateSessionRequest body) {
        Long userId = requireCaller();
        String title = body == null ? null : body.title();
        AiConsultSession s = consultService.createSession(userId, title);
        return ApiResponse.ok(SessionView.from(s, 0L));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('ai:consult')")
    public ApiResponse<PageResponse<SessionView>> listSessions(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        Long userId = requireCaller();
        Page<AiConsultSession> p = consultService.listSessions(userId, page, size);
        List<SessionView> rows = new ArrayList<>(p.getNumberOfElements());
        for (AiConsultSession s : p.getContent()) {
            rows.add(SessionView.from(s, consultService.countMessages(s.getId())));
        }
        return ApiResponse.ok(new PageResponse<>(rows, p.getTotalElements(), p.getNumber(), p.getSize()));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasAuthority('ai:consult')")
    public ApiResponse<SessionDetailView> getSession(@PathVariable Long id) {
        Long userId = requireCaller();
        AiConsultSession s = consultService.getSessionOwned(userId, id);
        List<AiConsultMessage> messages = consultService.listMessages(id);
        return ApiResponse.ok(SessionDetailView.from(s, messages));
    }

    @PatchMapping("/sessions/{id}")
    @PreAuthorize("hasAuthority('ai:consult')")
    public ApiResponse<SessionView> renameSession(@PathVariable Long id,
                                                  @Valid @RequestBody RenameSessionRequest body) {
        Long userId = requireCaller();
        AiConsultSession s = consultService.renameSession(userId, id, body.title());
        return ApiResponse.ok(SessionView.from(s, consultService.countMessages(s.getId())));
    }

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("hasAuthority('ai:consult')")
    public ApiResponse<Map<String, Object>> deleteSession(@PathVariable Long id) {
        Long userId = requireCaller();
        consultService.deleteSession(userId, id);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    // ---- SSE streaming -----------------------------------------------------

    /**
     * Stream the assistant reply for one new user message.
     *
     * <p>Pipeline:</p>
     * <ol>
     *   <li>Auth + row-level check on the session.</li>
     *   <li>Guardrail. Refusal → persist user msg + canned refusal msg →
     *       single SSE event + DONE; no LLM call.</li>
     *   <li>Persist user message.</li>
     *   <li>Build context (system primer + windowed history including the
     *       just-saved user message).</li>
     *   <li>Open streaming chat to upstream; flush deltas as
     *       {@code data: {"delta": "..."}} frames.</li>
     *   <li>On completion: persist assistant message + emit
     *       {@code {"done": true, "messageId": ..., "tokensIn": ..., "tokensOut": ...}}
     *       and {@code [DONE]}.</li>
     * </ol>
     *
     * <p>We use a blocking servlet write loop rather than returning a
     * {@code Flux<ServerSentEvent>} so the existing MVC security filter chain
     * (JWT, CORS, exception advice) applies unchanged. {@code response.flushBuffer()}
     * is called after every event so the client receives partial output
     * immediately. {@code X-Accel-Buffering: no} disables Nginx
     * response buffering.</p>
     */
    @PostMapping(value = "/sessions/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('ai:consult')")
    public void sendMessage(@PathVariable Long id,
                            @Valid @RequestBody SendMessageRequest body,
                            HttpServletResponse response) throws IOException {
        Long userId = requireCaller();
        AiConsultSession session = consultService.getSessionOwned(userId, id);

        // Prepare SSE response headers BEFORE any write.
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        // Critical when Nginx fronts the app — otherwise it buffers the body and
        // the client gets the full payload at the end instead of streaming.
        response.setHeader("X-Accel-Buffering", "no");

        PrintWriter writer = response.getWriter();

        String userContent = body.content().trim();

        // 1) Guardrail.
        GuardrailResult guard = guardrail.check(userContent);
        if (!guard.allowed()) {
            // Still persist both turns so users see the refusal in history.
            consultService.saveUserMessage(session.getId(), userContent);
            AiConsultMessage refusal = consultService.saveAssistantMessage(
                    session.getId(),
                    guard.refusalMessage(),
                    0, 0, null,
                    AiConsultMessage.STATUS_REFUSED,
                    "guardrail:" + guard.refusalMessage());
            consultService.touchSession(session.getId(), userContent);

            writeEvent(writer, Map.of("delta", guard.refusalMessage()));
            writeEvent(writer, Map.of(
                    "done", true,
                    "refused", true,
                    "messageId", refusal.getId(),
                    "tokensIn", 0,
                    "tokensOut", 0));
            writeDone(writer);
            return;
        }

        // 2) Persist user turn.
        consultService.saveUserMessage(session.getId(), userContent);

        // 3) Build context from full session history (now includes the new user msg).
        List<AiConsultMessage> history = consultService.listMessages(session.getId());
        List<ChatMessage> contextMessages = contextBuilder.build(history);

        ChatRequest chatRequest = ChatRequest.builder()
                .feature("consult")
                .model(properties.getChatModel())
                .messages(contextMessages)
                .estimatedTokens(estimateContextTokens(contextMessages))
                .build();

        StringBuilder assistantBuf = new StringBuilder();
        AtomicReference<Integer> finalTokensIn = new AtomicReference<>();
        AtomicReference<Integer> finalTokensOut = new AtomicReference<>();
        AtomicReference<Throwable> streamError = new AtomicReference<>();

        try {
            // Block on the reactive stream from the MVC thread. Acceptable because
            // we're already holding a long-lived servlet connection for the SSE
            // response; introducing a worker pool here would only add complexity.
            aiCallTemplate.chatStream(chatRequest)
                    .doOnNext(chunk -> {
                        try {
                            if (chunk.done()) {
                                if (chunk.tokensIn() != null) finalTokensIn.set(chunk.tokensIn());
                                if (chunk.tokensOut() != null) finalTokensOut.set(chunk.tokensOut());
                                return;
                            }
                            String delta = chunk.deltaContent();
                            if (delta == null || delta.isEmpty()) return;
                            assistantBuf.append(delta);
                            writeEvent(writer, Map.of("delta", delta));
                        } catch (IOException ioe) {
                            throw new RuntimeException(ioe);
                        }
                    })
                    .blockLast();
        } catch (AiRateLimitException ex) {
            handleStreamFailure(writer, session.getId(), assistantBuf, ex,
                    "rate_limited", "AI 调用频率超限：" + ex.getReason());
            return;
        } catch (RuntimeException ex) {
            streamError.set(ex.getCause() != null ? ex.getCause() : ex);
            handleStreamFailure(writer, session.getId(), assistantBuf, streamError.get(),
                    "upstream_error", "AI 服务暂时不可用，请稍后再试");
            return;
        }

        // 4) Persist assistant message with accumulated tokens.
        Integer tokensIn = finalTokensIn.get();
        Integer tokensOut = finalTokensOut.get();
        AiConsultMessage saved = consultService.saveAssistantMessage(
                session.getId(),
                assistantBuf.toString(),
                tokensIn, tokensOut,
                properties.getChatModel(),
                AiConsultMessage.STATUS_COMPLETED,
                null);
        consultService.touchSession(session.getId(), userContent);

        writeEvent(writer, Map.of(
                "done", true,
                "messageId", saved.getId(),
                "tokensIn", tokensIn == null ? 0 : tokensIn,
                "tokensOut", tokensOut == null ? 0 : tokensOut));
        writeDone(writer);
    }

    // ---- helpers -----------------------------------------------------------

    private Long requireCaller() {
        Long userId = currentUserResolver.currentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证用户禁止访问");
        }
        return userId;
    }

    private void handleStreamFailure(PrintWriter writer,
                                     Long sessionId,
                                     StringBuilder partial,
                                     Throwable err,
                                     String reason,
                                     String userFacing) throws IOException {
        log.warn("AI consult streaming failed (session={}, reason={}): {}", sessionId, reason, err.toString());
        AiConsultMessage failed = consultService.saveAssistantMessage(
                sessionId,
                partial.length() == 0 ? userFacing : partial.toString(),
                0, 0,
                properties.getChatModel(),
                AiConsultMessage.STATUS_FAILED,
                reason + ": " + err.getMessage());
        writeEvent(writer, Map.of("error", userFacing));
        writeEvent(writer, Map.of(
                "done", true,
                "failed", true,
                "messageId", failed.getId(),
                "tokensIn", 0,
                "tokensOut", 0));
        writeDone(writer);
    }

    private void writeEvent(PrintWriter writer, Map<String, Object> payload) throws IOException {
        try {
            writer.write("data: " + objectMapper.writeValueAsString(payload) + "\n\n");
        } catch (JsonProcessingException jpe) {
            throw new IOException(jpe);
        }
        writer.flush();
    }

    private static void writeDone(PrintWriter writer) {
        writer.write("data: [DONE]\n\n");
        writer.flush();
    }

    static int estimateContextTokens(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage m : messages) {
            total += m.contentAsText().length();
        }
        return Math.min(8000, total + 200);
    }

    // ---- DTOs --------------------------------------------------------------

    public record CreateSessionRequest(@Size(max = 200) String title) {}

    public record RenameSessionRequest(@NotBlank @Size(max = 200) String title) {}

    public record SendMessageRequest(@NotBlank @Size(max = 4000) String content) {}

    public record SessionView(Long id, String title, Instant updatedAt, Instant createdAt, long messageCount) {
        static SessionView from(AiConsultSession s, long messageCount) {
            return new SessionView(s.getId(), s.getTitle(), s.getUpdatedAt(), s.getCreatedAt(), messageCount);
        }
    }

    public record MessageView(Long id, String role, String content, Integer tokensIn, Integer tokensOut,
                              String model, String status, Instant createdAt) {
        static MessageView from(AiConsultMessage m) {
            return new MessageView(m.getId(), m.getRole(), m.getContent(),
                    m.getTokensIn(), m.getTokensOut(), m.getModel(), m.getStatus(), m.getCreatedAt());
        }
    }

    public record SessionDetailView(Long id, String title, Instant updatedAt, Instant createdAt, List<MessageView> messages) {
        static SessionDetailView from(AiConsultSession s, List<AiConsultMessage> messages) {
            List<MessageView> mapped = new ArrayList<>(messages.size());
            for (AiConsultMessage m : messages) mapped.add(MessageView.from(m));
            return new SessionDetailView(s.getId(), s.getTitle(), s.getUpdatedAt(), s.getCreatedAt(), mapped);
        }
    }
}
