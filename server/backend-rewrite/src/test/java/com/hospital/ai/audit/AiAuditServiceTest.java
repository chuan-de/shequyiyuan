package com.hospital.ai.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hospital.ai.client.ChatMessage;
import com.hospital.ai.client.ChatRequest;
import com.hospital.ai.client.ChatResponse;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Hand-rolled in-memory {@link AiAuditLogRepository} double — avoids Mockito's
 * inline mock-maker (which JDK 25 forbids in this project).
 */
class AiAuditServiceTest {

    @Test
    void recordSuccess_truncatesPromptAndResponseTo500CharsWithMarker() {
        RecordingRepo repo = new RecordingRepo();
        AiAuditService svc = new AiAuditService(repo, new org.springframework.beans.factory.ObjectProvider<>() {
            @Override public io.micrometer.core.instrument.MeterRegistry getObject() { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getObject(Object... args) { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getIfAvailable() { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getIfUnique() { return null; }
        });

        String hugePrompt = "p".repeat(2000);
        String hugeResp = "r".repeat(2000);

        ChatRequest req = ChatRequest.builder()
                .feature("consult")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", hugePrompt)))
                .build();
        ChatResponse resp = new ChatResponse("doubao-seed-2.0-lite", hugeResp, 100, 50, 1234);

        svc.recordSuccess(7L, req, resp);

        assertThat(repo.saved).hasSize(1);
        AiAuditLog saved = repo.saved.get(0);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getFeature()).isEqualTo("consult");
        assertThat(saved.getStatus()).isEqualTo("success");
        assertThat(saved.getTokensIn()).isEqualTo(100);
        assertThat(saved.getTokensOut()).isEqualTo(50);
        assertThat(saved.getLatencyMs()).isEqualTo(1234);
        // Prompt excerpt must be 500 chars + "..." marker.
        assertThat(saved.getPromptExcerpt()).hasSize(503).endsWith("...");
        assertThat(saved.getResponseExcerpt()).hasSize(503).endsWith("...");
    }

    @Test
    void recordFailure_capturesErrorMessageAndStatus() {
        RecordingRepo repo = new RecordingRepo();
        AiAuditService svc = new AiAuditService(repo, new org.springframework.beans.factory.ObjectProvider<>() {
            @Override public io.micrometer.core.instrument.MeterRegistry getObject() { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getObject(Object... args) { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getIfAvailable() { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getIfUnique() { return null; }
        });

        ChatRequest req = ChatRequest.builder()
                .feature("vision")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", "ocr please")))
                .build();

        svc.recordFailure(9L, req, 800, new RuntimeException("upstream 502"));

        AiAuditLog saved = repo.saved.get(0);
        assertThat(saved.getStatus()).isEqualTo("failed");
        assertThat(saved.getErrorMsg()).isEqualTo("upstream 502");
        assertThat(saved.getLatencyMs()).isEqualTo(800);
    }

    @Test
    void recordRateLimited_marksRowWithoutCallingUpstream() {
        RecordingRepo repo = new RecordingRepo();
        AiAuditService svc = new AiAuditService(repo, new org.springframework.beans.factory.ObjectProvider<>() {
            @Override public io.micrometer.core.instrument.MeterRegistry getObject() { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getObject(Object... args) { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getIfAvailable() { return null; }
            @Override public io.micrometer.core.instrument.MeterRegistry getIfUnique() { return null; }
        });

        ChatRequest req = ChatRequest.builder()
                .feature("patient-rag")
                .model("doubao-seed-2.0-lite")
                .messages(List.of(ChatMessage.text("user", "q")))
                .build();

        svc.recordRateLimited(3L, req, "qpm");

        AiAuditLog saved = repo.saved.get(0);
        assertThat(saved.getStatus()).isEqualTo("rate_limited");
        assertThat(saved.getErrorMsg()).contains("qpm");
    }

    @Test
    void truncate_leavesShortContentUntouched() {
        assertThat(AiAuditService.truncate("abc")).isEqualTo("abc");
        assertThat(AiAuditService.truncate(null)).isNull();
    }

    /** Minimum {@link AiAuditLogRepository} implementation that only honours save(). */
    static class RecordingRepo implements AiAuditLogRepository {
        final List<AiAuditLog> saved = new ArrayList<>();

        @Override public <S extends AiAuditLog> S save(S entity) { saved.add(entity); return entity; }

        // --- everything below is unused by AiAuditService ---
        @Override public List<AiAuditLog> findAll() { throw new UnsupportedOperationException(); }
        @Override public List<AiAuditLog> findAll(Sort sort) { throw new UnsupportedOperationException(); }
        @Override public Page<AiAuditLog> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public List<AiAuditLog> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public <S extends AiAuditLog> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<AiAuditLog> findById(Long id) { return Optional.empty(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public long count() { return saved.size(); }
        @Override public void deleteById(Long id) {}
        @Override public void delete(AiAuditLog entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends AiAuditLog> entities) {}
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override public <S extends AiAuditLog> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends AiAuditLog> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<AiAuditLog> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public AiAuditLog getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public AiAuditLog getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public AiAuditLog getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends AiAuditLog> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends AiAuditLog> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends AiAuditLog> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends AiAuditLog> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends AiAuditLog> long count(Example<S> example) { return 0; }
        @Override public <S extends AiAuditLog> boolean exists(Example<S> example) { return false; }
        @Override public <S extends AiAuditLog, R> R findBy(Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
