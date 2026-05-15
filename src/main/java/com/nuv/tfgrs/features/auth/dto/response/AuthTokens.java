package com.nuv.tfgrs.features.auth.dto.response;

public record AuthTokens(
        AuthResponse authResponse,
        String refreshToken
) {}
