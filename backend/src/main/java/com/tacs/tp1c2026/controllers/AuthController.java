package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.input.LoginDTO;
import com.tacs.tp1c2026.entities.dto.input.RegisterDTO;
import com.tacs.tp1c2026.entities.dto.output.AuthResponseDto;
import com.tacs.tp1c2026.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginDTO loginDTO) {
        return ResponseEntity.ok(authService.login(loginDTO));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody RegisterDTO registerDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerDTO));
    }
}
