package com.hospital.visit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
class VisitControllerIntegrationTests {

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
                                {"username":"%s","password":"123456","fullName":"就诊患者-%s"}
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data").get("id").asLong();
    }

    @Test
    void list_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/visits")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"visits:read"})
    void create_withReadOnlyAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"visits:read", "visits:write", "visits:delete", "patients:write"})
    void crudFlow_withDbPagination_shouldSucceed() throws Exception {
        long patientId = createPatient("v_pat_crud");

        // visitNumber 留空由服务端生成；患者信息由 JOIN 反规范化带出。
        String created = mockMvc.perform(post("/api/v1/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"fee":35.50,"keshiTypes":1,"registrationNotes":"初诊"}
                                """.formatted(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientId").value(patientId))
                .andExpect(jsonPath("$.data.visitNumber").isString())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = objectMapper.readTree(created).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"fee":50,"keshiTypes":2}
                                """.formatted(patientId)))
                .andExpect(status().isOk());

        // 数据库分页：total 与页大小无关。
        mockMvc.perform(get("/api/v1/visits?page=1&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(1));

        // patientId 过滤（360 视图时间线依赖）。
        mockMvc.perform(get("/api/v1/visits?patientId=" + patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(put("/api/v1/visits/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fee":40,"keshiTypes":1,"registrationNotes":"复诊改约"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrationNotes").value("复诊改约"));

        mockMvc.perform(delete("/api/v1/visits/" + id))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/visits/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"visits:write"})
    void create_forNonexistentPatient_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/v1/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":999999}
                                """))
                .andExpect(status().isNotFound());
    }
}
