package com.nuv.tfgrs.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Value;
import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.response.AuthResponse;
import com.nuv.tfgrs.entity.Usuario;
import com.nuv.tfgrs.entity.Usuario.Role;
import com.nuv.tfgrs.repository.UsuarioRepository;
import com.nuv.tfgrs.security.JwtService;
import com.nuv.tfgrs.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse registrar(AuthRequest.Register request) {
        if (usuarioRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("El usuario ya existe: " + request.username());
        }

        var usuario = Usuario.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Usuario.Role.ROLE_VIEWER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        usuarioRepository.save(usuario);
        log.info("Nuevo usuario registrado: {}", usuario.getUsername());

        String token = jwtService.generarToken(usuario);
        return new AuthResponse(token, usuario.getUsername(), usuario.getRole().name(), 86400000L);
    }

    @Override
    public AuthResponse login(AuthRequest.Login request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        var usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow();

        String token = jwtService.generarToken(usuario);
        log.info("Login exitoso: {}", usuario.getUsername());
        return new AuthResponse(token, usuario.getUsername(), usuario.getRole().name(), 86400000L);
    }

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    public AuthResponse loginConGoogle(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) throw new IllegalArgumentException("Token de Google inválido");

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId  = payload.getSubject();
            String email     = payload.getEmail();
            String avatarUrl = (String) payload.get("picture");

            // Upsert: find by googleId or email, else create
            Usuario usuario = usuarioRepository.findByGoogleId(googleId)
                .or(() -> usuarioRepository.findByUsername(email))
                .orElseGet(() -> {
                    Usuario nuevo = new Usuario();
                    nuevo.setUsername(email);
                    nuevo.setEmail(email);
                    nuevo.setPasswordHash(null);
                    nuevo.setRole(Role.ROLE_VIEWER); // default role for new OAuth users
                    return nuevo;
                });

            usuario.setGoogleId(googleId);
            usuario.setAvatarUrl(avatarUrl);
            usuarioRepository.save(usuario);

            String token = jwtService.generarToken(usuario);
            return new AuthResponse(token, usuario.getUsername(),
                                    usuario.getRole().name(), 86400000L);

        } catch (Exception e) {
            throw new IllegalArgumentException("Error verificando token de Google: " + e.getMessage());
        }
    }
}
