package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.response.AuthTokens;

public interface AuthService {
    AuthTokens registrar(AuthRequest.Register request);
    AuthTokens login(AuthRequest.Login request);
    AuthTokens loginConGoogle(String credential);
    AuthTokens refresh(String refreshToken);
}
