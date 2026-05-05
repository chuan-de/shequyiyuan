package com.hospital.dictionary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("hospital_test")
        .withUsername("hospital")
        .withPassword("hospital");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywaySeed_shouldLoadDictionaryData() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from dictionary", Integer.class);
        org.assertj.core.api.Assertions.assertThat(count).isNotNull().isGreaterThan(0);
    }

    @Test
    void getDictionaries_shouldReturnDictionaryCategories() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].dictCode").exists());
    }

    @Test
    void getDictionaryItems_shouldReturnItemsByDictCode() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/GENDER/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemCode").exists());
    }

    @Test
    void getDictionaryItems_notExistDictCode_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/dictionaries/NOT_EXISTS/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }
}
