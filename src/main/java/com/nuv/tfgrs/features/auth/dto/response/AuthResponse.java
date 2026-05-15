package com.nuv.tfgrs.features.auth.dto.response;

public record AuthResponse(
    String token,
    String username,
    String role,
    long expiresIn,
    boolean mustChangePassword,
    String timeZone
) {}
