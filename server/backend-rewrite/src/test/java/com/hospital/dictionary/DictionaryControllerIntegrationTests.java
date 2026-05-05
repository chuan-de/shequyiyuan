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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class DictionaryControllerIntegrationTests {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("hospital_test").withUsername("hospital").withPassword("hospital");
    @DynamicPropertySource static void configureProperties(DynamicPropertyRegistry registry) { registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword); }
    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void dictionaryCrudFlow_shouldSucceed() throws Exception {
        String body = """
            {"dictCode":"test_types","dictName":"测试类型","itemCode":"a1","itemName":"选项A","sortOrder":1,"enabled":true}
            """;
        String created = mockMvc.perform(post("/api/v1/dictionaries").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dictCode").value("test_types"))
            .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/v1/dictionaries/item/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.itemCode").value("a1"));

        mockMvc.perform(put("/api/v1/dictionaries/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("""{"dictCode":"test_types","dictName":"测试类型","itemCode":"a1","itemName":"选项A-更新","sortOrder":2,"enabled":true}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemName").value("选项A-更新"));

        mockMvc.perform(delete("/api/v1/dictionaries/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/dictionaries/item/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
    }
}
