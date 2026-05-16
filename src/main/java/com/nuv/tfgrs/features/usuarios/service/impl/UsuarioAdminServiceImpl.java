package com.nuv.tfgrs.features.usuarios.service.impl;

import com.nuv.tfgrs.features.usuarios.dto.request.ActualizarRolUsuarioRequest;
import com.nuv.tfgrs.features.usuarios.dto.request.InvitarUsuarioRequest;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioAdminResponse;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioInviteResponse;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioRoleAuditResponse;
import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership;
import com.nuv.tfgrs.features.usuarios.entity.Usuario;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.features.organizaciones.repository.OrganizationMembershipRepository;
import com.nuv.tfgrs.features.usuarios.repository.UsuarioRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.features.usuarios.service.UsuarioAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioAdminServiceImpl implements UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> listar() {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        return organizationMembershipRepository.findByOrganizationIdOrderByUserUsernameAsc(organizationId)
                .stream()
                .map(OrganizationMembership::getUser)
                .map(UsuarioAdminResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioInviteResponse invitar(InvitarUsuarioRequest request, String actorUsername) {
        currentOrganizationResolver.requireOwnerOrAdmin();
        var context = currentOrganizationResolver.requireMembership();
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya existe");
        }

        String temporaryPassword = generateTempPassword();
        Usuario.Role role = parseRole(request.role());

        Usuario usuario = Usuario.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .nombre(request.nombre().trim())
                .apellidos(request.apellidos().trim())
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .role(role)
                .active(true)
                .mustChangePassword(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Usuario saved = usuarioRepository.save(usuario);
        organizationMembershipRepository.save(OrganizationMembership.builder()
                .organization(context.currentOrganization())
                .user(saved)
                .role(mapToMembershipRole(role))
                .build());
        recordRoleChange(saved.getId(), actorUsername, Usuario.Role.ROLE_VIEWER, role, "INVITE");

        return new UsuarioInviteResponse(UsuarioAdminResponse.from(saved), temporaryPassword);
    }

    @Override
    @Transactional
    public UsuarioAdminResponse actualizarRol(Long userId, ActualizarRolUsuarioRequest request, String actorUsername) {
        currentOrganizationResolver.requireOwnerOrAdmin();
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Usuario usuario = findByIdOrThrow(userId, organizationId);
        Usuario.Role oldRole = usuario.getRole();
        Usuario.Role newRole = parseRole(request.role());

        usuario.setRole(newRole);
        usuario.setUpdatedAt(LocalDateTime.now());
        Usuario saved = usuarioRepository.save(usuario);

        recordRoleChange(saved.getId(), actorUsername, oldRole, newRole, request.reason());
        return UsuarioAdminResponse.from(saved);
    }

    @Override
    @Transactional
    public UsuarioAdminResponse desactivar(Long userId, String actorUsername) {
        currentOrganizationResolver.requireOwnerOrAdmin();
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Usuario usuario = findByIdOrThrow(userId, organizationId);
        if (actorUsername.equalsIgnoreCase(usuario.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propio usuario");
        }

        usuario.setActive(false);
        bumpTokenVersion(usuario);
        usuario.setUpdatedAt(LocalDateTime.now());
        return UsuarioAdminResponse.from(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioAdminResponse activar(Long userId, String actorUsername) {
        currentOrganizationResolver.requireOwnerOrAdmin();
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Usuario usuario = findByIdOrThrow(userId, organizationId);
        usuario.setActive(true);
        usuario.setUpdatedAt(LocalDateTime.now());
        return UsuarioAdminResponse.from(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioRoleAuditResponse> auditoria(int limit) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String sql = """
                SELECT
                    a.id,
                    a.user_id,
                    u.username,
                    a.changed_by,
                    actor.username AS changed_by_username,
                    a.old_role,
                    a.new_role,
                    a.reason,
                    a.changed_at
                FROM user_role_audit a
                JOIN users u ON u.id = a.user_id
                JOIN organization_memberships om ON om.user_id = u.id
                LEFT JOIN users actor ON actor.id = a.changed_by
                WHERE om.organization_id = :organizationId
                ORDER BY a.changed_at DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("limit", safeLimit)
                        .addValue("organizationId", organizationId), (rs, rowNum) ->
                new UsuarioRoleAuditResponse(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getObject("changed_by") != null ? rs.getLong("changed_by") : null,
                        rs.getString("changed_by_username"),
                        rs.getString("old_role"),
                        rs.getString("new_role"),
                        rs.getString("reason"),
                        rs.getTimestamp("changed_at").toLocalDateTime()
                )
        );
    }

    private Usuario findByIdOrThrow(Long userId, Long organizationId) {
        OrganizationMembership membership = organizationMembershipRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));
        if (!membership.getOrganization().getId().equals(organizationId)) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + userId);
        }
        return membership.getUser();
    }

    private Usuario.Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }

        String normalized = role.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        try {
            return Usuario.Role.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol inválido. Usa ADMIN, MANAGER o VIEWER", ex);
        }
    }

    private void recordRoleChange(Long userId, String actorUsername, Usuario.Role oldRole, Usuario.Role newRole, String reason) {
        Long actorId = usuarioRepository.findByUsername(actorUsername).map(Usuario::getId).orElse(null);

        String sql = """
                INSERT INTO user_role_audit (user_id, changed_by, old_role, new_role, reason, changed_at)
                VALUES (:userId, :changedBy, :oldRole, :newRole, :reason, NOW())
                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("changedBy", actorId)
                .addValue("oldRole", oldRole.name())
                .addValue("newRole", newRole.name())
                .addValue("reason", normalizeReason(reason))
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String value = reason.trim();
        return value.isEmpty() ? null : value;
    }

    private String generateTempPassword() {
        String seed = UUID.randomUUID().toString().replace("-", "");
        return "Tmp" + seed.substring(0, 6) + "9A";
    }

    private OrganizationMembership.MembershipRole mapToMembershipRole(Usuario.Role role) {
        return switch (role) {
            case ROLE_ADMIN, ROLE_MANAGER -> OrganizationMembership.MembershipRole.ADMIN;
            case ROLE_VIEWER -> OrganizationMembership.MembershipRole.MEMBER;
        };
    }

    private void bumpTokenVersion(Usuario usuario) {
        int current = usuario.getTokenVersion() == null ? 0 : usuario.getTokenVersion();
        usuario.setTokenVersion(current + 1);
    }
}
