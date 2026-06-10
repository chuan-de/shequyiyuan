package com.hospital.dictionary;

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
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class DictionaryControllerIntegrationTests {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")).withDatabaseName("hospital_test").withUsername("hospital").withPassword("hospital");
    @DynamicPropertySource static void configureProperties(DynamicPropertyRegistry registry) { registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword); }
    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", authorities = {"dictionary:read", "dictionary:write"})
    void dictionaryItemCrudFlow_shouldSucceed() throws Exception {
        // V3/V46 种子数据可见
        mockMvc.perform(get("/api/v1/dictionaries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.code=='sex_types')]").exists());

        String body = """
            {"dictCode":"test_types","dictName":"测试类型","itemCode":"1","itemName":"选项A","sortOrder":1,"enabled":true}
            """;
        String created = mockMvc.perform(post("/api/v1/dictionaries/items").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.value").value("1"))
            .andExpect(jsonPath("$.data.name").value("选项A"))
            .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        // 业务消费端点返回新建项
        mockMvc.perform(get("/api/v1/dictionaries/test_types/items/enabled"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("选项A"));

        // 更新
        mockMvc.perform(put("/api/v1/dictionaries/items/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"dictCode\":\"test_types\",\"dictName\":\"测试类型\",\"itemCode\":\"1\",\"itemName\":\"选项A-更新\",\"sortOrder\":2,\"enabled\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("选项A-更新"));

        // 停用后从 enabled 端点消失
        mockMvc.perform(patch("/api/v1/dictionaries/items/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enabled").value(false));
        mockMvc.perform(get("/api/v1/dictionaries/test_types/items/enabled"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"dictionary:write"})
    void createDuplicateItemCode_shouldReturn409() throws Exception {
        String body = """
            {"dictCode":"dup_types","dictName":"重复测试","itemCode":"1","itemName":"项","sortOrder":1,"enabled":true}
            """;
        mockMvc.perform(post("/api/v1/dictionaries/items").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/dictionaries/items").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void listEnabled_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/sex_types/items/enabled"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "patient", authorities = {"profile:read"})
    void listEnabled_withAnyAuthenticatedUser_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/sex_types/items/enabled"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].value").value("1"))
            .andExpect(jsonPath("$.data[0].name").value("男"));
    }

    @Test
    @WithMockUser(username = "patient", authorities = {"profile:read"})
    void createItem_withoutWriteAuthority_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1/dictionaries/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dictCode":"x_types","dictName":"X","itemCode":"1","itemName":"项","sortOrder":1,"enabled":true}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"dictionary:write"})
    void updateItem_forNonexistentId_shouldReturn404() throws Exception {
        mockMvc.perform(put("/api/v1/dictionaries/items/999999").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dictCode":"x_types","dictName":"X","itemCode":"1","itemName":"项","sortOrder":1,"enabled":true}
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"dictionary:write"})
    void createItem_withInvalidRequest_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/dictionaries/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dictCode":"","dictName":"测试类型","itemCode":"1","itemName":"项","sortOrder":-1,"enabled":true}
                    """))
            .andExpect(status().isBadRequest());
    }
}
