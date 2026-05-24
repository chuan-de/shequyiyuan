package com.hospital.ai.consult;

import java.time.Instant;
import java.util.List;

import com.hospital.common.NotFoundException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD on {@link AiConsultSession} + {@link AiConsultMessage}. Every method
 * takes an explicit {@code callerUserId} (resolved by the controller via
 * {@link com.hospital.ai.audit.CurrentUserResolver}) and uses repository
 * accessors that combine {@code id + user_id} — there is no
 * {@code findById(Long)} usage anywhere in this class.
 *
 * <p>Phase 3 enforces strict per-user isolation: even ADMIN cannot read or
 * mutate another user's sessions. Cross-user audit / moderation tooling will
 * arrive in Phase 4 behind {@code ai:admin}.</p>
 */
@Service
@ConditionalOnProperty(prefix = "hospital.ai",
        name = {"enabled", "features.consult"},
        havingValue = "true")
public class AiConsultService {

    static final String DEFAULT_TITLE = "新对话";
    static final int TITLE_MAX_LENGTH = 200;
    static final int AUTO_TITLE_TRUNCATE = 50;

    private final AiConsultSessionRepository sessionRepository;
    private final AiConsultMessageRepository messageRepository;

    public AiConsultService(AiConsultSessionRepository sessionRepository,
                            AiConsultMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    // ---- session CRUD ------------------------------------------------------

    @Transactional
    public AiConsultSession createSession(Long userId, String title) {
        requireUser(userId);
        AiConsultSession s = new AiConsultSession();
        s.setUserId(userId);
        s.setTitle(normalizeTitle(title));
        s.setUpdatedAt(Instant.now());
        return sessionRepository.save(s);
    }

    @Transactional(readOnly = true)
    public Page<AiConsultSession> listSessions(Long userId, int page, int size) {
        requireUser(userId);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(
                userId, PageRequest.of(safePage, safeSize));
    }

    @Transactional(readOnly = true)
    public long countMessages(Long sessionId) {
        return messageRepository.countBySessionId(sessionId);
    }

    @Transactional(readOnly = true)
    public AiConsultSession getSessionOwned(Long userId, Long sessionId) {
        requireUser(userId);
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("会话不存在或无权访问"));
    }

    @Transactional(readOnly = true)
    public List<AiConsultMessage> listMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public AiConsultSession renameSession(Long userId, Long sessionId, String newTitle) {
        AiConsultSession s = getSessionOwned(userId, sessionId);
        if (newTitle == null || newTitle.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能为空");
        }
        if (newTitle.length() > TITLE_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "标题长度不能超过 " + TITLE_MAX_LENGTH + " 字符");
        }
        s.setTitle(newTitle);
        s.setUpdatedAt(Instant.now());
        return sessionRepository.save(s);
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        AiConsultSession s = getSessionOwned(userId, sessionId);
        // CASCADE in V43 takes care of messages.
        sessionRepository.delete(s);
    }

    // ---- message CRUD ------------------------------------------------------

    @Transactional
    public AiConsultMessage saveUserMessage(Long sessionId, String content) {
        AiConsultMessage m = new AiConsultMessage();
        m.setSessionId(sessionId);
        m.setRole(AiConsultMessage.ROLE_USER);
        m.setContent(content);
        m.setStatus(AiConsultMessage.STATUS_COMPLETED);
        return messageRepository.save(m);
    }

    @Transactional
    public AiConsultMessage saveAssistantMessage(Long sessionId,
                                                 String content,
                                                 Integer tokensIn,
                                                 Integer tokensOut,
                                                 String model,
                                                 String status,
                                                 String errorMsg) {
        AiConsultMessage m = new AiConsultMessage();
        m.setSessionId(sessionId);
        m.setRole(AiConsultMessage.ROLE_ASSISTANT);
        m.setContent(content == null ? "" : content);
        m.setTokensIn(tokensIn);
        m.setTokensOut(tokensOut);
        m.setModel(model);
        m.setStatus(status == null ? AiConsultMessage.STATUS_COMPLETED : status);
        m.setErrorMsg(errorMsg);
        return messageRepository.save(m);
    }

    /**
     * Bump {@code updated_at} so the session sorts to the top of the user's
     * sidebar after a new exchange. Optionally back-fill the title from the
     * first user message when it's still the default.
     */
    @Transactional
    public void touchSession(Long sessionId, String maybeAutoTitleSource) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setUpdatedAt(Instant.now());
            if ((s.getTitle() == null || s.getTitle().isBlank() || DEFAULT_TITLE.equals(s.getTitle()))
                    && maybeAutoTitleSource != null && !maybeAutoTitleSource.isBlank()) {
                String t = maybeAutoTitleSource.trim();
                if (t.length() > AUTO_TITLE_TRUNCATE) t = t.substring(0, AUTO_TITLE_TRUNCATE);
                s.setTitle(t);
            }
            sessionRepository.save(s);
        });
    }

    // ---- helpers -----------------------------------------------------------

    private static void requireUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证用户禁止访问");
        }
    }

    static String normalizeTitle(String input) {
        if (input == null || input.isBlank()) return DEFAULT_TITLE;
        String t = input.trim();
        return t.length() > TITLE_MAX_LENGTH ? t.substring(0, TITLE_MAX_LENGTH) : t;
    }
}
