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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MembershipRoleAuthorityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void membershipRoleShouldControlAuthoritiesInsteadOfUsuarioRoleCache() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String username = "membership_" + suffix;
        String email = "membership_" + suffix + "@test.local";
        String password = "Password9A!";

        Map<String, Object> registerPayload = Map.of(
                "username", username,
                "email", email,
                "password", password,
                "nombre", "Membership",
                "apellidos", "Authority"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registerPayload)))
                .andExpect(status().isOk())
                .andReturn();

        String token = tokenFrom(registerResult);
        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "Org " + suffix))))
                .andExpect(status().isCreated());

        jdbcTemplate.update(
                """
                UPDATE users u
                JOIN organization_memberships om ON om.user_id = u.id
                SET u.role = 'ROLE_ADMIN', om.role = 'MEMBER'
                WHERE u.username = :username
                """,
                new MapSqlParameterSource("username", username)
        );

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "nombre", "NoAccess " + suffix,
                                "descripcion", "member should be denied"
                        ))))
                .andExpect(status().isForbidden());

        jdbcTemplate.update(
                """
                UPDATE users u
                JOIN organization_memberships om ON om.user_id = u.id
                SET u.role = 'ROLE_VIEWER', om.role = 'ADMIN'
                WHERE u.username = :username
                """,
                new MapSqlParameterSource("username", username)
        );

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "nombre", "CanAccess " + suffix,
                                "descripcion", "admin membership should be allowed"
                        ))))
                .andExpect(status().isCreated());
    }

    private String tokenFrom(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
