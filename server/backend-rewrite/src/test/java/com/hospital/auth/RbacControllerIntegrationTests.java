package com.hospital.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
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
class RbacControllerIntegrationTests {

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

    private long roleIdOf(String roleCode) throws Exception {
        String body = mockMvc.perform(get("/api/v1/rbac/roles"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode role : objectMapper.readTree(body).get("data")) {
            if (roleCode.equals(role.get("roleCode").asText())) return role.get("id").asLong();
        }
        throw new IllegalStateException("Role not seeded: " + roleCode);
    }

    @Test
    void listRoles_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/rbac/roles")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"medications:read"})
    void listRoles_withoutRbacRead_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/rbac/roles")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"rbac:read"})
    void updateRolePermissions_withReadOnlyAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(put("/api/v1/rbac/roles/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":["medications:read"]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"rbac:read"})
    void listPermissions_shouldContainSeededCodes() throws Exception {
        String body = mockMvc.perform(get("/api/v1/rbac/permissions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(body).get("data");
        boolean hasRbacWrite = false;
        for (JsonNode p : data) {
            if ("rbac:write".equals(p.get("code").asText())) hasRbacWrite = true;
        }
        org.junit.jupiter.api.Assertions.assertTrue(hasRbacWrite, "rbac:write should be seeded by V53");
    }

    @Test
    @WithMockUser(authorities = {"rbac:read", "rbac:write"})
    void updateAdminRole_shouldReturn409() throws Exception {
        long adminId = roleIdOf("ADMIN");
        mockMvc.perform(put("/api/v1/rbac/roles/" + adminId + "/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":[]}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = {"rbac:read", "rbac:write"})
    void updateWithUnknownPermissionCode_shouldReturn409() throws Exception {
        long receptionId = roleIdOf("RECEPTION");
        mockMvc.perform(put("/api/v1/rbac/roles/" + receptionId + "/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":["no-such-module:read"]}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = {"rbac:read", "rbac:write"})
    void replaceRolePermissions_shouldPersistExactly() throws Exception {
        long receptionId = roleIdOf("RECEPTION");
        mockMvc.perform(put("/api/v1/rbac/roles/" + receptionId + "/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":["medications:read","visits:read"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("RECEPTION"))
                .andExpect(jsonPath("$.data.permissionCodes.length()").value(2));

        String body = mockMvc.perform(get("/api/v1/rbac/roles"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode role : objectMapper.readTree(body).get("data")) {
            if (role.get("id").asLong() == receptionId) {
                org.junit.jupiter.api.Assertions.assertEquals(2, role.get("permissionCodes").size());
            }
        }
    }

    @Test
    @WithMockUser(authorities = {"rbac:read", "rbac:write"})
    void updateNonexistentRole_shouldReturn404() throws Exception {
        mockMvc.perform(put("/api/v1/rbac/roles/999999/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":["medications:read"]}
                                """))
                .andExpect(status().isNotFound());
    }
}
