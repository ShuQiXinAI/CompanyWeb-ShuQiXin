package com.shuqixin.auth.controller;

import com.shuqixin.auth.dto.ApiResponse;
import com.shuqixin.auth.dto.AuthRequest;
import com.shuqixin.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody AuthRequest request) {
        ApiResponse response = authService.register(request);
        return ResponseEntity.status(response.getCode() == 200 ? 200 : response.getCode()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody AuthRequest request) {
        ApiResponse response = authService.login(request);
        return ResponseEntity.status(response.getCode() == 200 ? 200 : response.getCode()).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ApiResponse response = authService.me(authHeader);
        return ResponseEntity.status(response.getCode() == 200 ? 200 : response.getCode()).body(response);
    }
}
