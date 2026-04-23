package com.nuv.tfgrs.dto.response;

public record AuthTokens(
        AuthResponse authResponse,
        String refreshToken
) {}
