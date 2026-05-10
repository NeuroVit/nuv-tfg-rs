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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrganizationOnboardingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void session_shouldRequireOnboarding_whenUserHasNoOrganizationMembership() throws Exception {
        String username = "noorg_" + System.nanoTime();
        insertUserWithoutMembership(username, username + "@test.local");

        MvcResult result = mockMvc.perform(get("/api/auth/session")
                        .with(user(username).roles("VIEWER")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(body.path("requiresOrganizationOnboarding").asBoolean());
        assertTrue(body.path("organization").isNull());
    }

    @Test
    void createOrganization_shouldCreateOwnerMembershipAndDisableOnboarding() throws Exception {
        String username = "founder_" + System.nanoTime();
        insertUserWithoutMembership(username, username + "@test.local");

        MvcResult createResult = mockMvc.perform(post("/api/organizations")
                        .with(user(username).roles("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("name", "Acme Clinic"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsByteArray());
        assertEquals("acme-clinic", createBody.path("organization").path("slug").asText());
        assertEquals("owner", createBody.path("membership").path("role").asText());

        MvcResult sessionResult = mockMvc.perform(get("/api/auth/session")
                        .with(user(username).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode sessionBody = objectMapper.readTree(sessionResult.getResponse().getContentAsByteArray());
        assertFalse(sessionBody.path("requiresOrganizationOnboarding").asBoolean());
        assertEquals("owner", sessionBody.path("role").asText());
        assertEquals("Acme Clinic", sessionBody.path("organization").path("name").asText());
    }

    @Test
    void crossOrganizationAccess_shouldBeBlockedForProducts() throws Exception {
        Long orgB = insertOrganization("Org B", "org-b-" + System.nanoTime());
        String username = "tenantb_" + System.nanoTime();
        Long userId = insertUserWithoutMembership(username, username + "@test.local");
        insertMembership(orgB, userId, "MEMBER");

        mockMvc.perform(get("/api/productos/1")
                        .with(user(username).roles("VIEWER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchAndJoinOrganization_shouldCreateMemberMembershipAndDisableOnboarding() throws Exception {
        Long targetOrganizationId = insertOrganization("Acme Search Org", "acme-search-org-" + System.nanoTime());
        String username = "joiner_" + System.nanoTime();
        insertUserWithoutMembership(username, username + "@test.local", "ROLE_ADMIN");

        MvcResult searchResult = mockMvc.perform(get("/api/organizations/search")
                        .param("query", "Acme Search")
                        .with(user(username).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode searchBody = objectMapper.readTree(searchResult.getResponse().getContentAsByteArray());
        boolean containsTarget = false;
        for (JsonNode row : searchBody) {
            if (row.path("id").asLong() == targetOrganizationId) {
                containsTarget = true;
                break;
            }
        }
        assertTrue(containsTarget);

        MvcResult joinResult = mockMvc.perform(post("/api/organizations/join")
                        .with(user(username).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("organizationId", targetOrganizationId))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode joinBody = objectMapper.readTree(joinResult.getResponse().getContentAsByteArray());
        assertEquals("member", joinBody.path("membership").path("role").asText());
        assertEquals(targetOrganizationId.longValue(), joinBody.path("organization").path("id").asLong());

        String roleInDb = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE username = :username",
                new MapSqlParameterSource("username", username),
                String.class
        );
        assertEquals("ROLE_VIEWER", roleInDb);

        MvcResult sessionResult = mockMvc.perform(get("/api/auth/session")
                        .with(user(username).roles("VIEWER")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode sessionBody = objectMapper.readTree(sessionResult.getResponse().getContentAsByteArray());
        assertFalse(sessionBody.path("requiresOrganizationOnboarding").asBoolean());
        assertEquals("member", sessionBody.path("role").asText());
        assertEquals(targetOrganizationId.longValue(), sessionBody.path("organization").path("id").asLong());
    }

    @Test
    void joinOrganization_shouldReturnConflict_whenUserAlreadyHasMembership() throws Exception {
        Long currentOrg = insertOrganization("Current Org", "current-org-" + System.nanoTime());
        Long otherOrg = insertOrganization("Other Org", "other-org-" + System.nanoTime());
        String username = "already_member_" + System.nanoTime();
        Long userId = insertUserWithoutMembership(username, username + "@test.local");
        insertMembership(currentOrg, userId, "MEMBER");

        mockMvc.perform(post("/api/organizations/join")
                        .with(user(username).roles("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("organizationId", otherOrg))))
                .andExpect(status().isConflict());
    }

    @Test
    void joinOrganization_shouldReturnNotFound_whenOrganizationDoesNotExist() throws Exception {
        String username = "missing_org_" + System.nanoTime();
        insertUserWithoutMembership(username, username + "@test.local");

        mockMvc.perform(post("/api/organizations/join")
                        .with(user(username).roles("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("organizationId", Long.MAX_VALUE))))
                .andExpect(status().isNotFound());
    }

    @Test
    void organizationOwnedData_shouldBeFilteredByCurrentOrganization() throws Exception {
        Long orgB = insertOrganization("Org C", "org-c-" + System.nanoTime());
        insertClient(orgB, "Cliente Org C");

        MvcResult result = mockMvc.perform(get("/api/clientes")
                        .param("includeArchived", "true")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        for (JsonNode row : body) {
            assertNotNull(row.path("nombre").asText(null));
            assertFalse("Cliente Org C".equals(row.path("nombre").asText()));
        }
    }

    private Long insertUserWithoutMembership(String username, String email) {
        return insertUserWithoutMembership(username, email, "ROLE_VIEWER");
    }

    private Long insertUserWithoutMembership(String username, String email, String role) {
        String sql = """
                INSERT INTO users (username, email, password_hash, role, is_active, must_change_password, created_at, updated_at)
                VALUES (:username, :email, :passwordHash, :role, 1, 0, NOW(), NOW())
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("email", email)
                .addValue("passwordHash", passwordEncoder.encode("Password9A"))
                .addValue("role", role), keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        assertNotNull(key);
        return key.longValue();
    }

    private Long insertOrganization(String name, String slug) {
        String sql = """
                INSERT INTO organizations (name, slug, created_at, updated_at)
                VALUES (:name, :slug, NOW(), NOW())
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("slug", slug), keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        assertNotNull(key);
        return key.longValue();
    }

    private void insertMembership(Long organizationId, Long userId, String role) {
        jdbcTemplate.update(
                """
                INSERT INTO organization_memberships (organization_id, user_id, role, created_at, updated_at)
                VALUES (:organizationId, :userId, :role, NOW(), NOW())
                """,
                new MapSqlParameterSource()
                        .addValue("organizationId", organizationId)
                        .addValue("userId", userId)
                        .addValue("role", role)
        );
    }

    private void insertClient(Long organizationId, String name) {
        jdbcTemplate.update(
                """
                INSERT INTO clients (organization_id, name, email, phone, notes, is_active, created_at, updated_at)
                VALUES (:organizationId, :name, :email, '600000001', 'test', 1, NOW(), NOW())
                """,
                new MapSqlParameterSource()
                        .addValue("organizationId", organizationId)
                        .addValue("name", name)
                        .addValue("email", "client-" + System.nanoTime() + "@test.local")
        );
    }
}
