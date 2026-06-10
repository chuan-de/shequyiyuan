package com.hospital.familydoctor;

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
class ContractControllerIntegrationTests {

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
                                {"username":"%s","password":"123456","fullName":"测试患者-%s"}
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("id").asLong();
    }

    private long createFamilyDoctor(String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/family-doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"123456","fullName":"测试家医-%s"}
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("id").asLong();
    }

    @Test
    void list_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/family-doctor-contracts")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"family-doctor-contracts:read"})
    void create_withReadOnlyAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/family-doctor-contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":1,"familyDoctorId":1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"family-doctor-contracts:read", "family-doctor-contracts:write",
            "patients:write", "family-doctors:write"})
    void create_secondActiveContractForSamePatient_shouldReturn409() throws Exception {
        long patientId = createPatient("c_pat_dup");
        long doctorId = createFamilyDoctor("c_fd_dup");

        mockMvc.perform(post("/api/v1/family-doctor-contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"familyDoctorId":%d,"servicePackage":"基础包"}
                                """.formatted(patientId, doctorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 一患者同一时间只允许一份生效中的签约。
        mockMvc.perform(post("/api/v1/family-doctor-contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"familyDoctorId":%d}
                                """.formatted(patientId, doctorId)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = {"family-doctor-contracts:read", "family-doctor-contracts:write",
            "patients:write", "family-doctors:write"})
    void cancelThenRecontract_shouldSucceed() throws Exception {
        long patientId = createPatient("c_pat_re");
        long doctorId = createFamilyDoctor("c_fd_re");

        String created = mockMvc.perform(post("/api/v1/family-doctor-contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"familyDoctorId":%d}
                                """.formatted(patientId, doctorId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long contractId = objectMapper.readTree(created).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/family-doctor-contracts/" + contractId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetStatus":"TERMINATED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TERMINATED"));

        mockMvc.perform(post("/api/v1/family-doctor-contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"familyDoctorId":%d}
                                """.formatted(patientId, doctorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = {"family-doctor-contracts:read", "family-doctor-contracts:write",
            "patients:write", "family-doctors:write"})
    void list_filterByPatientId_shouldOnlyReturnThatPatient() throws Exception {
        long patientId = createPatient("c_pat_filter");
        long doctorId = createFamilyDoctor("c_fd_filter");
        mockMvc.perform(post("/api/v1/family-doctor-contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":%d,"familyDoctorId":%d}
                                """.formatted(patientId, doctorId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/family-doctor-contracts?patientId=" + patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].patientId").value(patientId));
    }

    @Test
    @WithMockUser(authorities = {"family-doctor-contracts:write"})
    void create_forNonexistentPatient_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/v1/family-doctor-contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":999999,"familyDoctorId":999999}
                                """))
                .andExpect(status().isNotFound());
    }
}
