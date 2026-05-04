package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.response.UserProfileResponse;
import com.nuv.tfgrs.dto.response.AuthTokens;

public interface AuthService {
    AuthTokens registrar(AuthRequest.Register request);
    AuthTokens login(AuthRequest.Login request);
    AuthTokens loginConGoogle(String credential);
    AuthTokens refresh(String refreshToken);
    AuthTokens cambiarRolUsuarioActual(String username, String role);
    UserProfileResponse perfilUsuarioActual(String username);
    UserProfileResponse actualizarPerfilUsuarioActual(String username, AuthRequest.UpdateMeRequest request);
    void cambiarPasswordUsuarioActual(String username, AuthRequest.ChangePasswordRequest request);
}
