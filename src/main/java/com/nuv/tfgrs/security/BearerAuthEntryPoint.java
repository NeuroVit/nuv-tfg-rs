package com.nuv.tfgrs.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BearerAuthEntryPoint implements AuthenticationEntryPoint {

    public static final String INVALID_TOKEN_REQUEST_ATTRIBUTE = "auth.invalid_token";

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        if (Boolean.TRUE.equals(request.getAttribute(INVALID_TOKEN_REQUEST_ATTRIBUTE))) {
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
