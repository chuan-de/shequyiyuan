package com.hospital.patient;

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
class PatientControllerIntegrationTests {

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
        mockMvc.perform(get("/api/v1/patients")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"patients:read"})
    void create_withReadOnlyAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"patient1","password":"secret1","fullName":"张三"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"patients:read", "patients:write", "patients:status"})
    void crudFlow_shouldSucceed() throws Exception {
        String created = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"patient_crud","password":"secret1","fullName":"李四","phone":"13800138001","idNumber":"110101199001011234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("李四"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn().getResponse().getContentAsString();

        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(put("/api/v1/patients/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"李四（更新）","phone":"13800138001","idNumber":"110101199001011234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("李四（更新）"));

        mockMvc.perform(patch("/api/v1/patients/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @WithMockUser(authorities = {"patients:read"})
    void detail_forNonexistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/patients/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"patients:read", "patients:write"})
    void create_withMissingRequiredFields_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"123456","fullName":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
