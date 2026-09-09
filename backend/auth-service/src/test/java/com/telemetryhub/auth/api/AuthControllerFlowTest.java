package com.telemetryhub.auth.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.telemetryhub.auth.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class AuthControllerFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Enregistrement tenant -> login -> refresh -> logout : flux complet")
    void fullAuthFlow() throws Exception {
        // 1. Register tenant
        String registerBody = """
                {
                  "tenantName": "Acme Industries",
                  "slug": "acme",
                  "adminEmail": "admin@acme.com",
                  "adminFullName": "Admin Acme",
                  "password": "SuperSecretPassword123"
                }
                """;

        String registerJson = mockMvc.perform(post("/api/v1/auth/register-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("admin@acme.com"))
                .andExpect(jsonPath("$.user.role").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(registerJson).get("accessToken").asText();
        String refreshToken = objectMapper.readTree(registerJson).get("refreshToken").asText();

        // 2. Login
        String loginBody = """
                {
                  "email": "admin@acme.com",
                  "password": "SuperSecretPassword123",
                  "deviceName": "test-device"
                }
                """;

        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String loginAccessToken = objectMapper.readTree(loginJson).get("accessToken").asText();
        assertThat(loginAccessToken).isNotBlank();

        // 3. Refresh rotation
        String refreshBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        String refreshJson = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode refreshResponse = objectMapper.readTree(refreshJson);
        assertThat(refreshResponse.get("refreshToken").asText()).isNotEqualTo(refreshToken);

        // 4. Logout
        String newRefresh = refreshResponse.get("refreshToken").asText();
        String logoutBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(newRefresh);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isNoContent());

        // 5. Refresh with revoked token must fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(newRefresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Mauvais mot de passe -> 401")
    void wrongPasswordRejected() throws Exception {
        String registerBody = """
                {
                  "tenantName": "Beta Corp",
                  "slug": "beta",
                  "adminEmail": "admin@beta.com",
                  "adminFullName": "Admin Beta",
                  "password": "SuperSecretPassword123"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/register-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {
                  "email": "admin@beta.com",
                  "password": "wrong-password-123",
                  "deviceName": "test"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    @DisplayName("Slug dupliqué -> 409")
    void duplicateSlugRejected() throws Exception {
        String registerBody = """
                {
                  "tenantName": "Gamma",
                  "slug": "gamma",
                  "adminEmail": "admin1@gamma.com",
                  "adminFullName": "Admin Gamma",
                  "password": "SuperSecretPassword123"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/register-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String duplicateBody = registerBody.replace("admin1@gamma.com", "admin2@gamma.com");
        mockMvc.perform(post("/api/v1/auth/register-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"));
    }
}