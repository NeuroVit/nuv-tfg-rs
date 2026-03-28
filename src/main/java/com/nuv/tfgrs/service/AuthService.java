package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse registrar(AuthRequest.Register request);
    AuthResponse login(AuthRequest.Login request);
}