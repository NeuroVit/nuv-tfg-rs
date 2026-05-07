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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthInvitationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void inviteUser_shouldSetMustChangePassword_andLoginShouldExposeFlag() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String username = "invited_" + suffix;
        String email = "invited_" + suffix + "@test.local";

        Map<String, Object> payload = Map.of(
                "username", username,
                "email", email,
                "nombre", "Invitado",
                "apellidos", "Temporal",
                "role", "ROLE_VIEWER"
        );

        MvcResult inviteResult = mockMvc.perform(post("/api/usuarios/invite")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode inviteBody = objectMapper.readTree(inviteResult.getResponse().getContentAsByteArray());
        String temporaryPassword = inviteBody.path("temporaryPassword").asText();
        assertNotNull(temporaryPassword);
        assertFalse(temporaryPassword.isBlank());

        Boolean mustChangePasswordDb = jdbcTemplate.queryForObject(
                "SELECT must_change_password FROM users WHERE username = :username",
                new MapSqlParameterSource("username", username),
                Boolean.class
        );
        assertTrue(Boolean.TRUE.equals(mustChangePasswordDb));

        Map<String, Object> loginPayload = Map.of(
                "username", username,
                "password", temporaryPassword
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
        assertTrue(loginBody.path("mustChangePassword").asBoolean());
    }

    @Test
    void changePassword_shouldClearMustChangePasswordFlag() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String username = "invited_" + suffix;
        String email = "invited_" + suffix + "@test.local";

        Map<String, Object> payload = Map.of(
                "username", username,
                "email", email,
                "nombre", "Invitado",
                "apellidos", "Temporal",
                "role", "ROLE_VIEWER"
        );

        MvcResult inviteResult = mockMvc.perform(post("/api/usuarios/invite")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode inviteBody = objectMapper.readTree(inviteResult.getResponse().getContentAsByteArray());
        String temporaryPassword = inviteBody.path("temporaryPassword").asText();

        Map<String, Object> changePasswordPayload = Map.of(
                "currentPassword", temporaryPassword,
                "newPassword", "NuevaClave9A"
        );

        mockMvc.perform(post("/api/auth/change-password")
                        .with(user(username).roles("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(changePasswordPayload)))
                .andExpect(status().isNoContent());

        Boolean mustChangePasswordDb = jdbcTemplate.queryForObject(
                "SELECT must_change_password FROM users WHERE username = :username",
                new MapSqlParameterSource("username", username),
                Boolean.class
        );
        assertFalse(Boolean.TRUE.equals(mustChangePasswordDb));

        Map<String, Object> loginPayload = Map.of(
                "username", username,
                "password", "NuevaClave9A"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
        assertFalse(loginBody.path("mustChangePassword").asBoolean());
    }
}
