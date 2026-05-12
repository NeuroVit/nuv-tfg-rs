package com.nuv.tfgrs.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "auth.role-switch.enabled=true")
@Transactional
class TokenRevocationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void logout_shouldBumpTokenVersion_andInvalidateAccessToken() throws Exception {
        AuthFixture fixture = registerUser("logout");
        int before = tokenVersion(fixture.username());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(fixture.token())))
                .andExpect(status().isNoContent());

        int after = tokenVersion(fixture.username());
        assertTrue(after > before);
        assertUnauthorizedMe(fixture.token());
    }

    @Test
    void changePassword_shouldBumpTokenVersion_andInvalidateAccessToken() throws Exception {
        AuthFixture fixture = registerUser("pwd");
        int before = tokenVersion(fixture.username());

        Map<String, Object> payload = Map.of(
                "currentPassword", fixture.password(),
                "newPassword", "NewPass9A!"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isNoContent());

        int after = tokenVersion(fixture.username());
        assertTrue(after > before);
        assertUnauthorizedMe(fixture.token());
    }

    @Test
    void selfRoleChange_shouldBumpTokenVersion_andInvalidateAccessToken() throws Exception {
        AuthFixture fixture = registerUser("role");
        jdbcTemplate.update(
                "UPDATE users SET role = 'ROLE_MANAGER', updated_at = NOW() WHERE username = :username",
                new MapSqlParameterSource("username", fixture.username())
        );

        int before = tokenVersion(fixture.username());

        Map<String, Object> payload = Map.of("role", "VIEWER");
        mockMvc.perform(post("/api/auth/role")
                        .header("Authorization", bearer(fixture.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk());

        int after = tokenVersion(fixture.username());
        assertTrue(after > before);
        assertUnauthorizedMe(fixture.token());
    }

    @Test
    void adminDeactivate_shouldBumpTokenVersion_forTargetUser() throws Exception {
        AuthFixture fixture = registerUser("deactivate");
        int before = tokenVersion(fixture.username());
        Long userId = userId(fixture.username());
        assertNotNull(userId);

        mockMvc.perform(patch("/api/usuarios/{id}/deactivate", userId)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        int after = tokenVersion(fixture.username());
        assertTrue(after > before);
        assertUnauthorizedMe(fixture.token());
    }

    private AuthFixture registerUser(String prefix) throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String username = prefix + "_" + suffix;
        String password = "Pass9A!" + prefix;
        String email = prefix + "_" + suffix + "@test.local";

        Map<String, Object> payload = Map.of(
                "username", username,
                "email", email,
                "password", password,
                "nombre", "Test",
                "apellidos", "User"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        String token = body.path("token").asText();
        assertFalse(token.isBlank());
        return new AuthFixture(username, password, token);
    }

    private void assertUnauthorizedMe(String token) throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    private int tokenVersion(String username) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT token_version FROM users WHERE username = :username",
                new MapSqlParameterSource("username", username),
                Integer.class
        );
        return value == null ? 0 : value;
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = :username",
                new MapSqlParameterSource("username", username),
                Long.class
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record AuthFixture(String username, String password, String token) {}
}
