package com.hospital.medication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class MedicationControllerIntegrationTests {

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

    @Test
    void list_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/medications")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"medications:read"})
    void create_withReadOnlyAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"MED001","name":"Aspirin"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"medications:read", "medications:write", "medications:status"})
    void crudFlow_shouldSucceed() throws Exception {
        String created = mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"MED001","name":"Aspirin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("MED001"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andReturn().getResponse().getContentAsString();

        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/v1/medications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(put("/api/v1/medications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"MED001","name":"Aspirin 500mg"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Aspirin 500mg"));

        mockMvc.perform(patch("/api/v1/medications/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    @WithMockUser(authorities = {"medications:read"})
    void detail_forNonexistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/medications/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"medications:read", "medications:write"})
    void create_withMissingRequiredFields_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/medications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
