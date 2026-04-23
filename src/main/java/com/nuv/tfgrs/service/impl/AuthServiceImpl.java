package com.nuv.tfgrs.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.response.AuthResponse;
import com.nuv.tfgrs.dto.response.AuthTokens;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

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

    @Value("${google.client-id}")
    private String googleClientId;

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
        try {
            log.info("Iniciando verificación de login con Google");
            log.debug("Google auth with clientId={}", googleClientId);

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_GOOGLE_ERROR);
            }

            Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String avatarUrl = (String) payload.get("picture");

            log.debug("Google token válido. GoogleId={}, Email={}", googleId, email);

            Usuario usuario = usuarioRepository.findByGoogleId(googleId)
                    .or(() -> usuarioRepository.findByEmail(email))
                    .orElseGet(() -> {
                        Usuario nuevo = new Usuario();
                        nuevo.setUsername(email);
                        nuevo.setEmail(email);
                        nuevo.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                        nuevo.setRole(Role.ROLE_VIEWER);
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
            throw ex;
        } catch (Exception ex) {
            log.warn("Fallo en autenticación con Google");
            log.debug("Google auth failed", ex);
            throw new AuthException(HttpStatus.UNAUTHORIZED, GENERIC_GOOGLE_ERROR, ex);
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

    private AuthTokens buildAuthTokens(Usuario usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);
        String refreshToken = jwtService.generarRefreshToken(usuario);
        AuthResponse authResponse = new AuthResponse(
                accessToken,
                usuario.getUsername(),
                usuario.getRole().name(),
                jwtService.getAccessExpirationMs()
        );
        return new AuthTokens(authResponse, refreshToken);
    }
}
