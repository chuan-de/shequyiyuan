package com.hospital.followup;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class FollowupControllerIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
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
    private ObjectMapper objectMapper;

    private long createPatient(String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"123456","fullName":"随访患者-%s"}
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("id").asLong();
    }

    @Test
    void list_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/followups")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"followups:read"})
    void create_withReadOnlyAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/followups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"followups:read", "followups:write", "patients:write"})
    void create_shouldComputeBmiServerSide() throws Exception {
        long patientId = createPatient("f_pat_bmi");
        // BMI = 65 / 1.70² = 22.49… → 保留 1 位小数 22.5
        mockMvc.perform(post("/api/v1/followups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"systolic":120,"diastolic":80,"heightCm":170,"weightKg":65}
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bmi").value(22.5))
                .andExpect(jsonPath("$.data.patientId").value(patientId));
    }

    @Test
    @WithMockUser(authorities = {"followups:read", "followups:write", "followups:delete", "patients:write"})
    void crudFlow_withDbPagination_shouldSucceed() throws Exception {
        long patientId = createPatient("f_pat_crud");
        long firstId = 0;
        for (int i = 0; i < 3; i++) {
            String body = mockMvc.perform(post("/api/v1/followups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"patientId":%d,"systolic":%d,"diastolic":80}
                                    """.formatted(patientId, 110 + i)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            if (i == 0) firstId = objectMapper.readTree(body).get("data").get("id").asLong();
        }

        // 数据库分页：total 不随页大小变化，页大小生效。
        mockMvc.perform(get("/api/v1/followups?patientId=" + patientId + "&page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(2));
        mockMvc.perform(get("/api/v1/followups?patientId=" + patientId + "&page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1));

        mockMvc.perform(put("/api/v1/followups/" + firstId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"systolic":130,"diastolic":85,"notes":"复查"}
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systolic").value(130))
                .andExpect(jsonPath("$.data.notes").value("复查"));

        mockMvc.perform(delete("/api/v1/followups/" + firstId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/followups/" + firstId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"followups:write"})
    void create_withOutOfRangeVitals_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/followups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1,"systolic":500}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"followups:write"})
    void create_forNonexistentPatient_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/v1/followups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":999999}
                                """))
                .andExpect(status().isNotFound());
    }
}
