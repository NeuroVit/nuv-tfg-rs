package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.response.AuthResponse;
import com.nuv.tfgrs.entity.Usuario;
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
}
