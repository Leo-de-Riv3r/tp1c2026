package com.tacs.tp1c2026.controllers;

import com.tacs.tp1c2026.entities.dto.user.input.LoginDTO;
import com.tacs.tp1c2026.entities.dto.user.input.RegisterDTO;
import com.tacs.tp1c2026.entities.dto.user.output.LoginResponseDto;
import com.tacs.tp1c2026.services.AuthService;
import com.tacs.tp1c2026.services.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;
  private final SessionService sessionService;

  public AuthController(AuthService authService, SessionService sessionService) {
    this.authService = authService;
    this.sessionService = sessionService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDTO dto) {
    return ResponseEntity.ok(authService.login(dto));
  }

  @PostMapping("/register")
  public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody RegisterDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      sessionService.delete(authHeader.substring(7));
    }
    return ResponseEntity.noContent().build();
  }
}
