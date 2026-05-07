package com.nuv.tfgrs.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.response.AuthResponse;
import com.nuv.tfgrs.dto.response.AuthTokens;
import com.nuv.tfgrs.dto.response.UserProfileResponse;
import com.nuv.tfgrs.entity.Usuario;
import com.nuv.tfgrs.entity.Usuario.Role;
import com.nuv.tfgrs.exception.AuthException;
import com.nuv.tfgrs.repository.UsuarioRepository;
import com.nuv.tfgrs.security.JwtService;
import com.nuv.tfgrs.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String GENERIC_REGISTER_ERROR = "No se pudo completar el registro";
    private static final String GENERIC_LOGIN_ERROR = "Credenciales inválidas";
    private static final String GENERIC_GOOGLE_ERROR = "No se pudo completar el inicio de sesión con Google";
    private static final String GENERIC_REFRESH_ERROR = "No se pudo renovar la sesión";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${auth.debug.google:false}")
    private boolean googleDebugEnabled;

    @Value("${auth.role-switch.enabled:true}")
    private boolean roleSwitchEnabled;

    @Override
    @Transactional
    public AuthTokens registrar(AuthRequest.Register request) {
        if (usuarioRepository.findByUsername(request.username()).isPresent()
                || usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, GENERIC_REGISTER_ERROR);
        }

        var usuario = Usuario.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nombre(request.nombre())
                .apellidos(request.apellidos())
                .role(Usuario.Role.ROLE_VIEWER)
                .active(true)
                .mustChangePassword(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Registro rechazado por conflicto de datos");
            log.debug("Registro rechazado para username={}, email={}", request.username(), request.email(), ex);
            throw new AuthException(HttpStatus.BAD_REQUEST, GENERIC_REGISTER_ERROR, ex);
        }

        log.info("Nuevo usuario registrado");
        log.debug("Nuevo usuario registrado: {}", usuario.getUsername());

        return buildAuthTokens(usuario);
    }

    @Override
    public AuthTokens login(AuthRequest.Login request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR, ex);
        }

        var usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR));

        log.info("Login exitoso");
        log.debug("Login exitoso: {}", usuario.getUsername());
        return buildAuthTokens(usuario);
    }

    @Override
    public AuthTokens loginConGoogle(String credential) {
        if (credential == null || credential.isBlank()) {
            log.warn("Google auth rechazado: credential vacío");
            throw new AuthException(HttpStatus.BAD_REQUEST, googleError("credential vacío"));
        }

        try {
            log.info("Iniciando verificación de login con Google");
            log.debug("Google auth config: clientId={}, tokenFingerprint={}",
                    googleClientId, tokenFingerprint(credential));
            log.debug("Google token hints (UNVERIFIED): {}", extractUnverifiedTokenHints(credential));

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                String detail = "verifier.verify devolvió null; posible mismatch de aud/client-id o token inválido";
                log.warn("Google auth falló: {}", detail);
                throw new AuthException(HttpStatus.UNAUTHORIZED, googleError(detail));
            }

            Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String avatarUrl = (String) payload.get("picture");

            Object audience = payload.getAudience();
            String issuer = payload.getIssuer();
            String azp = payload.getAuthorizedParty();
            log.debug("Google token válido. GoogleId={}, Email={}, aud={}, azp={}, iss={}",
                    googleId, email, audience, azp, issuer);

            Usuario usuario = usuarioRepository.findByGoogleId(googleId)
                    .or(() -> usuarioRepository.findByEmail(email))
                    .orElseGet(() -> {
                        Usuario nuevo = new Usuario();
                        nuevo.setUsername(email);
                        nuevo.setEmail(email);
                        nuevo.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                        nuevo.setRole(Role.ROLE_VIEWER);
                        nuevo.setActive(true);
                        nuevo.setMustChangePassword(false);
                        nuevo.setCreatedAt(LocalDateTime.now());
                        nuevo.setUpdatedAt(LocalDateTime.now());
                        return nuevo;
                    });

            usuario.setGoogleId(googleId);
            usuario.setAvatarUrl(avatarUrl);
            usuarioRepository.save(usuario);

            log.info("Usuario de Google actualizado/creado correctamente");
            log.debug("Usuario Google guardado: {}, role: {}", usuario.getUsername(), usuario.getRole());

            log.info("JWT generado correctamente");
            log.debug("JWT generated successfully for: {}", usuario.getUsername());
            return buildAuthTokens(usuario);
        } catch (AuthException ex) {
            if (googleDebugEnabled) {
                log.debug("Google auth AuthException detail: {}", ex.getMessage());
            }
            throw ex;
        } catch (Exception ex) {
            String detail = "Excepción " + ex.getClass().getSimpleName() + ": "
                    + (ex.getMessage() != null ? ex.getMessage() : "(sin mensaje)");
            log.warn("Fallo en autenticación con Google: {}", detail);
            log.debug("Google auth failed", ex);
            throw new AuthException(HttpStatus.UNAUTHORIZED, googleError(detail), ex);
        }
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_REFRESH_ERROR);
        }

        try {
            String username = jwtService.extraerUsername(refreshToken);
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_REFRESH_ERROR));

            if (!jwtService.esRefreshTokenValido(refreshToken, usuario)) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_REFRESH_ERROR);
            }

            log.info("Refresh token válido");
            log.debug("Refresh token válido para {}", usuario.getUsername());
            return buildAuthTokens(usuario);
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Fallo renovando sesión");
            log.debug("Error renovando sesión", ex);
            throw new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_REFRESH_ERROR, ex);
        }
    }

    @Override
    @Transactional
    public AuthTokens cambiarRolUsuarioActual(String username, String role) {
        if (!roleSwitchEnabled) {
            throw new AuthException(HttpStatus.FORBIDDEN, "El cambio de rol está deshabilitado");
        }

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR));

        Role nuevoRol = parseRole(role);
        Role oldRole = usuario.getRole();
        usuario.setRole(nuevoRol);
        usuario.setUpdatedAt(LocalDateTime.now());
        Usuario saved = usuarioRepository.save(usuario);
        recordRoleChange(saved.getId(), saved.getId(), oldRole, nuevoRol, "SELF_SWITCH");

        log.info("Rol de usuario actualizado");
        log.debug("Rol actualizado para {} -> {}", username, nuevoRol);
        return buildAuthTokens(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse perfilUsuarioActual(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR));
        return UserProfileResponse.from(usuario);
    }

    @Override
    @Transactional
    public UserProfileResponse actualizarPerfilUsuarioActual(String username, AuthRequest.UpdateMeRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR));

        String normalizedEmail = request.email().trim().toLowerCase();
        usuarioRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(usuario.getId()))
                .ifPresent(existing -> {
                    throw new AuthException(HttpStatus.CONFLICT, "El email ya está en uso");
                });

        usuario.setNombre(request.nombre().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setEmail(normalizedEmail);
        usuario.setUpdatedAt(LocalDateTime.now());

        Usuario saved = usuarioRepository.save(usuario);
        return UserProfileResponse.from(saved);
    }

    @Override
    @Transactional
    public void cambiarPasswordUsuarioActual(String username, AuthRequest.ChangePasswordRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_LOGIN_ERROR));

        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Tu cuenta no tiene contraseña local configurada");
        }

        if (!passwordEncoder.matches(request.currentPassword(), usuario.getPasswordHash())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta a la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        usuario.setMustChangePassword(false);
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    private void recordRoleChange(Long userId, Long changedBy, Role oldRole, Role newRole, String reason) {
        String sql = """
                INSERT INTO user_role_audit (user_id, changed_by, old_role, new_role, reason, changed_at)
                VALUES (:userId, :changedBy, :oldRole, :newRole, :reason, NOW())
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("changedBy", changedBy)
                .addValue("oldRole", oldRole.name())
                .addValue("newRole", newRole.name())
                .addValue("reason", reason)
        );
    }

    private AuthTokens buildAuthTokens(Usuario usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);
        String refreshToken = jwtService.generarRefreshToken(usuario);
        AuthResponse authResponse = new AuthResponse(
                accessToken,
                usuario.getUsername(),
                usuario.getRole().name(),
                jwtService.getAccessExpirationMs(),
                Boolean.TRUE.equals(usuario.getMustChangePassword())
        );
        return new AuthTokens(authResponse, refreshToken);
    }

    private String googleError(String detail) {
        if (!googleDebugEnabled) {
            return GENERIC_GOOGLE_ERROR;
        }
        return GENERIC_GOOGLE_ERROR + " [" + detail + "]";
    }

    private String tokenFingerprint(String token) {
        if (token == null || token.isBlank()) {
            return "empty";
        }
        int length = token.length();
        int head = Math.min(10, length);
        int tail = Math.min(8, Math.max(0, length - head));
        String prefix = token.substring(0, head);
        String suffix = token.substring(length - tail);
        return "len=" + length + ",prefix=" + prefix + ",suffix=" + suffix;
    }

    private String extractUnverifiedTokenHints(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return "formato JWT inválido";
            }
            String payloadJson = new String(Base64.decodeBase64(parts[1]), StandardCharsets.UTF_8);
            return "iss=" + fieldValue(payloadJson, "iss")
                    + ", aud=" + fieldValue(payloadJson, "aud")
                    + ", azp=" + fieldValue(payloadJson, "azp")
                    + ", sub=" + fieldValue(payloadJson, "sub")
                    + ", exp=" + fieldValue(payloadJson, "exp")
                    + ", iat=" + fieldValue(payloadJson, "iat")
                    + ", email=" + fieldValue(payloadJson, "email");
        } catch (Exception ex) {
            return "no se pudo parsear payload JWT: " + ex.getClass().getSimpleName();
        }
    }

    private String fieldValue(String json, String field) {
        Pattern stringPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher stringMatcher = stringPattern.matcher(json);
        if (stringMatcher.find()) {
            return stringMatcher.group(1);
        }

        Pattern numericPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*([0-9]+)");
        Matcher numericMatcher = numericPattern.matcher(json);
        if (numericMatcher.find()) {
            return numericMatcher.group(1);
        }

        Pattern arrayPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\\[(.*?)\\]");
        Matcher arrayMatcher = arrayPattern.matcher(json);
        if (arrayMatcher.find()) {
            return "[" + arrayMatcher.group(1).replace("\"", "") + "]";
        }

        return "null";
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }

        String normalized = role.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Rol inválido. Usa ADMIN, MANAGER o VIEWER", ex);
        }
    }
}
