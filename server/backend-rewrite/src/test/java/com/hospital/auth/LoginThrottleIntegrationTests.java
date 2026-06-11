package com.hospital.auth;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 登录失败限流端到端验证。max-attempts 压到 3，避免循环太多次；
 * 该 @DynamicPropertySource 组合会形成独立的 Spring 上下文，不污染其他测试类。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class LoginThrottleIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("hospital_test")
        .withUsername("hospital")
        .withPassword("hospital");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("security.login-throttle.max-attempts", () -> "3");
        registry.add("security.login-throttle.lock-duration", () -> "15m");
    }

    @Autowired
    private MockMvc mockMvc;

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"password123"}
                    """.formatted(username)))
            .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, password)));
    }

    @Test
    void exceedingFailedAttempts_shouldLockEvenWithCorrectPassword() throws Exception {
        register("locked_user");

        for (int i = 0; i < 3; i++) {
            login("locked_user", "wrong-password").andExpect(status().isUnauthorized());
        }

        // 锁定后即使密码正确也拒绝，并带 Retry-After
        login("locked_user", "password123")
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void otherUserFromSameIp_shouldNotBeAffected() throws Exception {
        register("victim_user");
        register("normal_user");

        for (int i = 0; i < 3; i++) {
            login("victim_user", "wrong-password").andExpect(status().isUnauthorized());
        }

        // 计数键含用户名：同 IP 下其他账号正常登录
        login("normal_user", "password123")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void successfulLoginBeforeThreshold_shouldResetCounter() throws Exception {
        register("reset_user");

        login("reset_user", "wrong-password").andExpect(status().isUnauthorized());
        login("reset_user", "wrong-password").andExpect(status().isUnauthorized());
        login("reset_user", "password123").andExpect(status().isOk());

        // 成功后清零：再错两次也不触发锁定
        login("reset_user", "wrong-password").andExpect(status().isUnauthorized());
        login("reset_user", "wrong-password").andExpect(status().isUnauthorized());
        login("reset_user", "password123").andExpect(status().isOk());
    }
}
