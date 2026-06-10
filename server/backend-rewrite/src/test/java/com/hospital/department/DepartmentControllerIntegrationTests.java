package com.hospital.department;

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
class DepartmentControllerIntegrationTests {

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

    @Test
    void list_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/departments")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"departments:read"})
    void create_withReadOnlyAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deptName":"内科"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"departments:read", "departments:write", "departments:delete"})
    void crudFlow_shouldSucceed() throws Exception {
        String created = mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deptName":"全科门诊","headPerson":"张主任","phone":"0571-12345678"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deptName").value("全科门诊"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/departments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headPerson").value("张主任"));

        mockMvc.perform(put("/api/v1/departments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deptName":"全科门诊","headPerson":"李主任"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headPerson").value("李主任"));

        mockMvc.perform(delete("/api/v1/departments/" + id))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/departments/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"departments:read", "departments:write"})
    void create_withDuplicateName_shouldReturn409() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deptName":"口腔科"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deptName":"口腔科"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = {"departments:read"})
    void detail_forNonexistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/departments/999999"))
                .andExpect(status().isNotFound());
    }
}
