package com.nuv.tfgrs.dto.response;

public record AuthResponse(
    String token,
    String username,
    String role,
    long expiresIn
) {}