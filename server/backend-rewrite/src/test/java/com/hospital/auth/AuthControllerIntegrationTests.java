package com.hospital.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTests {

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

    @Test
    void register_shouldReturn201() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"alice","password":"password123"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("registered"));
    }

    @Test
    void register_duplicate_shouldReturnError() throws Exception {
        String payload = """
            {"username":"bob","password":"password123"}
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void login_shouldReturnAccessToken_andMeEndpointShouldWork() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"charlie","password":"password123"}
                    """))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"charlie","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(
            loginResult.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("charlie"));
    }

    @Test
    void me_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void options_login_shouldNotBeBlocked() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void legacyPinyinRoutes_shouldRemainCompatibleDuringTransition() throws Exception {
        mockMvc.perform(post("/api/v1/yonghu/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"legacy_user","password":"password123"}
                    """))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/yonghu/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"legacy_user","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(
            loginResult.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/yonghu/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("legacy_user"));
    }
}
