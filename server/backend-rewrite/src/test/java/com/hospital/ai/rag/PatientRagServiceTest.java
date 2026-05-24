package com.hospital.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.hospital.ai.rag.PatientRagService.PatientRow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for PatientRagService prompt assembly + the role / row-level
 * authorisation rules. The retrieval path now goes through PatientKnowledgeStore
 * (Qdrant) — exercised separately in PatientKnowledgeStoreTest with a mocked
 * QdrantClient.
 */
class PatientRagServiceTest {

    private final PatientRagService svc = new PatientRagService(null, null, null, null, null, null);

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorize_admin_can_ask_for_any_patient() {
        login("admin", Set.of("ROLE_ADMIN", "ai:patient-rag"));
        svc.authorize(new PatientRow(1L, 999L, Instant.now()), 42L); // does not throw
    }

    @Test
    void authorize_doctor_can_ask_for_any_patient() {
        login("alice", Set.of("ROLE_DOCTOR", "ai:patient-rag"));
        svc.authorize(new PatientRow(1L, 999L, Instant.now()), 42L); // does not throw
    }

    @Test
    void authorize_patient_can_ask_about_themselves_only() {
        login("bob", Set.of("ROLE_PATIENT", "ai:patient-rag"));
        // bob is user 42 and owns patient profile 1 (user_id=42) — allowed.
        svc.authorize(new PatientRow(1L, 42L, Instant.now()), 42L);
        // Same bob trying patient 2 (user_id=43) — rejected.
        assertThrows(AccessDeniedException.class,
                () -> svc.authorize(new PatientRow(2L, 43L, Instant.now()), 42L));
    }

    @Test
    void authorize_unauthenticated_is_rejected() {
        // No login → authorities empty.
        assertThrows(AccessDeniedException.class,
                () -> svc.authorize(new PatientRow(1L, 999L, Instant.now()), 42L));
    }

    @Test
    void buildUserPrompt_includes_question_and_numbered_citations() {
        List<Citation> citations = List.of(
                new Citation(1L, "medical_record", 100L, "diagnosis",
                        "上呼吸道感染", Instant.parse("2026-01-01T00:00:00Z"), 0.92),
                new Citation(2L, "visit", 200L, "visit_content",
                        "复诊", Instant.parse("2026-02-01T00:00:00Z"), 0.81));
        String prompt = PatientRagService.buildUserPrompt("最近的诊断？", citations);

        assertTrue(prompt.contains("[#1]"));
        assertTrue(prompt.contains("[#2]"));
        assertTrue(prompt.contains("上呼吸道感染"));
        assertTrue(prompt.contains("最近的诊断？"));
    }

    @Test
    void buildUserPrompt_handles_empty_citations() {
        String prompt = PatientRagService.buildUserPrompt("有什么病史？", List.of());
        assertTrue(prompt.contains("（无）"));
        assertTrue(prompt.contains("有什么病史？"));
    }

    @Test
    void buildSystemPrompt_forbids_fabrication() {
        String sys = PatientRagService.buildSystemPrompt();
        assertTrue(sys.contains("不要编造"));
        assertEquals(true, sys.length() > 50);
    }

    private static void login(String username, Set<String> authorities) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                username, "x", authorities.stream().map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
