package com.nuv.tfgrs.controller;
 
import com.nuv.tfgrs.dto.request.AuthRequest;
import com.nuv.tfgrs.dto.response.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    // TODO: inject AuthService in Phase 2
 
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AuthRequest.Register request) {
        // TODO: implement in Phase 2
        return ResponseEntity.ok().build();
    }
 
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest.Login request) {
        // TODO: implement in Phase 2
        return ResponseEntity.ok().build();
    }
}
 