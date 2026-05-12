package com.nuv.tfgrs.integration;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleMatrixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void readEndpoints_shouldAllowViewerManagerAdmin() throws Exception {
        Long organizationId = organizationIdFor("admin");
        Long productId = firstProductIdWithStock(organizationId);

        for (RoleActor actor : actors()) {
            mockMvc.perform(get("/api/movimientos")
                            .with(actor.auth())
                            .param("limit", "5"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/ventas")
                            .with(actor.auth())
                            .param("limit", "5"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/productos/{id}", productId)
                            .with(actor.auth()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void writeEndpoints_shouldBlockViewer_andAllowManagerAdmin() throws Exception {
        Long organizationId = organizationIdFor("admin");
        Long categoryId = firstCategoryId(organizationId);
        Long providerId = firstProviderId(organizationId);
        Long clientId = firstActiveClientId(organizationId);
        Long saleProductId = firstProductIdWithStock(organizationId);

        RoleActor viewer = new RoleActor("viewer", "VIEWER");
        RoleActor manager = new RoleActor("manager", "MANAGER");
        RoleActor admin = new RoleActor("admin", "ADMIN");

        Map<String, Object> createForViewer = productCreatePayload("viewer", categoryId, providerId);
        mockMvc.perform(post("/api/productos")
                        .with(viewer.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createForViewer)))
                .andExpect(status().isForbidden());

        MvcResult managerCreate = mockMvc.perform(post("/api/productos")
                        .with(manager.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(productCreatePayload("manager", categoryId, providerId))))
                .andExpect(status().isCreated())
                .andReturn();
        Long managerProductId = readId(managerCreate);

        MvcResult adminCreate = mockMvc.perform(post("/api/productos")
                        .with(admin.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(productCreatePayload("admin", categoryId, providerId))))
                .andExpect(status().isCreated())
                .andReturn();
        Long adminProductId = readId(adminCreate);

        Map<String, Object> updatePayload = Map.of("nombre", "Updated " + System.nanoTime());
        mockMvc.perform(put("/api/productos/{id}", managerProductId)
                        .with(viewer.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updatePayload)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/productos/{id}", managerProductId)
                        .with(manager.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updatePayload)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/productos/{id}", adminProductId)
                        .with(admin.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updatePayload)))
                .andExpect(status().isOk());

        Map<String, Object> salePayload = Map.of(
                "clientId", clientId,
                "notas", "role-matrix",
                "items", List.of(Map.of(
                        "productId", saleProductId,
                        "quantity", 1,
                        "descuentoPct", BigDecimal.ZERO
                ))
        );

        mockMvc.perform(post("/api/ventas")
                        .with(viewer.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(salePayload)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ventas")
                        .with(manager.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(salePayload)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/ventas")
                        .with(admin.auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(salePayload)))
                .andExpect(status().isCreated());
    }

    private List<RoleActor> actors() {
        return List.of(
                new RoleActor("viewer", "VIEWER"),
                new RoleActor("manager", "MANAGER"),
                new RoleActor("admin", "ADMIN")
        );
    }

    private Long organizationIdFor(String username) {
        return jdbcTemplate.queryForObject(
                """
                SELECT om.organization_id
                FROM organization_memberships om
                JOIN users u ON u.id = om.user_id
                WHERE u.username = :username
                LIMIT 1
                """,
                new MapSqlParameterSource("username", username),
                Long.class
        );
    }

    private Long firstCategoryId(Long organizationId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE organization_id = :organizationId ORDER BY id LIMIT 1",
                new MapSqlParameterSource("organizationId", organizationId),
                Long.class
        );
    }

    private Long firstProviderId(Long organizationId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM providers WHERE organization_id = :organizationId ORDER BY id LIMIT 1",
                new MapSqlParameterSource("organizationId", organizationId),
                Long.class
        );
    }

    private Long firstActiveClientId(Long organizationId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM clients WHERE organization_id = :organizationId AND is_active = 1 ORDER BY id LIMIT 1",
                new MapSqlParameterSource("organizationId", organizationId),
                Long.class
        );
    }

    private Long firstProductIdWithStock(Long organizationId) {
        Long candidate = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM products
                WHERE organization_id = :organizationId
                  AND current_quantity > 20
                ORDER BY current_quantity DESC, id ASC
                LIMIT 1
                """,
                new MapSqlParameterSource("organizationId", organizationId),
                Long.class
        );
        if (candidate != null) {
            return candidate;
        }
        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM products
                WHERE organization_id = :organizationId
                  AND current_quantity > 0
                ORDER BY id
                LIMIT 1
                """,
                new MapSqlParameterSource("organizationId", organizationId),
                Long.class
        );
    }

    private Map<String, Object> productCreatePayload(String tag, Long categoryId, Long providerId) {
        String suffix = String.valueOf(System.nanoTime());
        return Map.of(
                "nombre", "Matrix Product " + tag + " " + suffix,
                "sku", "MATRIX-" + tag.toUpperCase() + "-" + suffix,
                "descripcion", "role matrix product",
                "precioUnitario", new BigDecimal("19.99"),
                "costoUnitario", new BigDecimal("9.99"),
                "cantidadActual", 15,
                "nivelMinimoStock", 3,
                "categoriaId", categoryId,
                "providerId", providerId
        );
    }

    private Long readId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("id")
                .asLong();
    }

    private record RoleActor(String username, String role) {
        SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor auth() {
            return SecurityMockMvcRequestPostProcessors.user(username).roles(role);
        }
    }
}
