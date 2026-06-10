package com.hospital.reception;

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
class ReceptionControllerIntegrationTests {

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
        mockMvc.perform(get("/api/v1/receptions")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"receptions:read"})
    void create_withReadOnlyAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"recept1","password":"secret1","fullName":"王五"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"receptions:read", "receptions:write", "receptions:status"})
    void crudFlow_shouldSucceed() throws Exception {
        String created = mockMvc.perform(post("/api/v1/receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"recept_crud","password":"secret1","fullName":"赵六","uuidNumber":"GZ001","phone":"13900139001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("赵六"))
                .andExpect(jsonPath("$.data.uuidNumber").value("GZ001"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // 不能用正则从 JSON 抠 id：getContentAsString() 默认按 ISO-8859-1 解码，
        // 中文 UTF-8 字节里会冒出 U+0085 行分隔符把贪婪 `.*` 截断，产出垃圾 id。
        long id = objectMapper.readTree(created).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/receptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(put("/api/v1/receptions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"赵六（更新）","uuidNumber":"GZ001","phone":"13900139001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("赵六（更新）"));

        mockMvc.perform(patch("/api/v1/receptions/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @WithMockUser(authorities = {"receptions:read"})
    void detail_forNonexistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/receptions/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"receptions:read", "receptions:write"})
    void create_withMissingRequiredFields_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/receptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"123456","fullName":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
