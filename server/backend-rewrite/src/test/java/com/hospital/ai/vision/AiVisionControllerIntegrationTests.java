package com.hospital.ai.vision;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.hospital.ai.common.AiRateLimitException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spec coverage:
 *
 * <table>
 *   <tr><th>case</th><th>expected status</th></tr>
 *   <tr><td>missing photoId + inline data</td><td>400</td></tr>
 *   <tr><td>unauthenticated</td><td>401</td></tr>
 *   <tr><td>authenticated without ai:vision</td><td>403</td></tr>
 *   <tr><td>rate limited (service throws AiRateLimitException)</td><td>429</td></tr>
 *   <tr><td>upstream failure (service throws RuntimeException)</td><td>500</td></tr>
 *   <tr><td>happy path</td><td>200 + parsed fields</td></tr>
 * </table>
 *
 * <p>{@link AiVisionService} is swapped with a stub via a nested
 * {@link TestConfiguration}; the real {@code DoubaoVisionService} is
 * exercised in unit tests for {@link MedicalRecordExtractor} — keeps these
 * tests laser-focused on routing, auth, serialisation, and error mapping.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "hospital.ai.enabled=true",
        "hospital.ai.features.vision=true",
        "hospital.ai.api-key=test-key-not-used"
})
@Import(AiVisionControllerIntegrationTests.StubServiceConfig.class)
class AiVisionControllerIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("hospital_test").withUsername("hospital").withPassword("hospital");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubVisionService stub;

    @Test
    void parse_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/vision/parse-medical-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"medical-records:read"})
    void parse_withoutAiVisionAuthority_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/ai/vision/parse-medical-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ai:vision"})
    void parse_withMissingPhotoIdAndInline_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/vision/parse-medical-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"ai:vision"})
    void parse_rateLimited_returns429() throws Exception {
        stub.exceptionToThrow.set(new AiRateLimitException("qpm", "exceeded"));

        mockMvc.perform(post("/api/v1/ai/vision/parse-medical-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("qpm"));
    }

    @Test
    @WithMockUser(authorities = {"ai:vision"})
    void parse_upstreamFailure_returns500() throws Exception {
        stub.exceptionToThrow.set(new RuntimeException("upstream boom"));

        mockMvc.perform(post("/api/v1/ai/vision/parse-medical-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(authorities = {"ai:vision"})
    void parse_happyPath_returns200WithParsedFields() throws Exception {
        stub.resultToReturn.set(new AiVisionResult(
                Map.of("patientName", "张三", "age", 45),
                new MedicalRecordFields("张三", "男", 45, "2025-03-12", "内科",
                        "咳嗽", "受凉", "上感", "阿莫西林", "李医生"),
                100.0, 350, 120, 2400L, 999L));

        mockMvc.perform(post("/api/v1/ai/vision/parse-medical-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fields.patientName").value("张三"))
                .andExpect(jsonPath("$.data.fields.age").value(45))
                .andExpect(jsonPath("$.data.tokensIn").value(350))
                .andExpect(jsonPath("$.data.tokensOut").value(120))
                .andExpect(jsonPath("$.data.extractionHistoryId").value(999));
    }

    // --- test wiring -------------------------------------------------------

    static class StubVisionService implements AiVisionService {
        final AtomicReference<AiVisionResult> resultToReturn = new AtomicReference<>();
        final AtomicReference<RuntimeException> exceptionToThrow = new AtomicReference<>();

        @Override
        public AiVisionResult extractMedicalRecord(VisionExtractRequest request) {
            RuntimeException ex = exceptionToThrow.getAndSet(null);
            if (ex != null) throw ex;
            AiVisionResult r = resultToReturn.getAndSet(null);
            if (r != null) return r;
            return new AiVisionResult(Map.of(), new MedicalRecordFields(null, null, null,
                    null, null, null, null, null, null, null), 0.0, 0, 0, 0L, 0L);
        }
    }

    @TestConfiguration
    static class StubServiceConfig {
        @Bean
        @Primary
        StubVisionService stubVisionService() {
            return new StubVisionService();
        }
    }
}
