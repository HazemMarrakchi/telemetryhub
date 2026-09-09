package com.telemetryhub.fleet.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.telemetryhub.fleet.FleetServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = FleetServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FleetControllerFlowTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String JWT_SECRET = "TEST_SECRET_KEY_THAT_IS_AT_LEAST_32_BYTES_LONG_FOR_TESTS";

    @Autowired
    private MockMvc mockMvc;

    private static String token() {
        String accessToken = JWT.create()
                .withIssuer("telemetryhub-auth")
                .withSubject("admin@acme.com")
                .withClaim("uid", "22222222-2222-2222-2222-222222222222")
                .withClaim("tid", TENANT_ID.toString())
                .withClaim("role", "TENANT_ADMIN")
                .withClaim("name", "Admin Acme")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(900))
                .sign(Algorithm.HMAC256(JWT_SECRET));
        return "Bearer " + accessToken;
    }

    @Test
    void createModelThenListAndCreateSiteAndEquipment() throws Exception {
        String modelBody = """
                {
                  "name": "Compresseur industriel",
                  "manufacturer": "Atlas Copco",
                  "description": "Compresseur d'air à vis",
                  "category": "ROTATING_EQUIPMENT"
                }
                """;

        String modelJson = mockMvc.perform(post("/api/v1/models")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modelBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Compresseur industriel"))
                .andReturn().getResponse().getContentAsString();

        UUID modelId = UUID.fromString(modelJson
                .replaceAll(".*\\\"id\\\":\\\"([0-9a-f-]{36})\\\".*", "$1"));

        String siteBody = """
                {
                  "name": "Usine de Sousse",
                  "address": "Route de la ceinture",
                  "city": "Sousse",
                  "country": "Tunisie",
                  "latitude": 35.8256,
                  "longitude": 10.6083
                }
                """;

        String siteJson = mockMvc.perform(post("/api/v1/sites")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(siteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Sousse"))
                .andExpect(jsonPath("$.latitude").value(35.8256))
                .andReturn().getResponse().getContentAsString();

        UUID siteId = UUID.fromString(siteJson
                .replaceAll(".*\\\"id\\\":\\\"([0-9a-f-]{36})\\\".*", "$1"));

        String equipmentBody = """
                {
                  "name": "Compresseur SAS-01",
                  "serialNumber": "SN-SAS-2026-001",
                  "modelId": "%s",
                  "siteId": "%s",
                  "notes": "Installé en production"
                }
                """.formatted(modelId, siteId);

        mockMvc.perform(post("/api/v1/equipments")
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equipmentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serialNumber").value("SN-SAS-2026-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/equipments")
                        .header("Authorization", token())
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Compresseur SAS-01"));

        mockMvc.perform(get("/api/v1/sites")
                        .header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void accessWithoutJwtIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/equipments"))
                .andExpect(status().isUnauthorized());
    }
}