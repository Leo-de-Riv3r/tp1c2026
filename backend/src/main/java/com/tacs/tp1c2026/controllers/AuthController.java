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

  /**
   * Login único (user y admin comparten flujo). Valida email + password contra Mongo y
   * devuelve el token de sesión + el {@link LoginResponseDto} con el UserDto.
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDTO dto) {
    return ResponseEntity.ok(authService.login(dto));
  }

  /**
   * Registra un user nuevo y lo deja logueado (token + UserDto), para que el FE no necesite
   * un segundo round-trip al login.
   */
  @PostMapping("/register")
  public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody RegisterDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
  }

  /**
   * Cierra la sesión: revoca la sesión server-side (borra el doc) si viene el token en el
   * header {@code Authorization}. Idempotente: 204 aunque el token no exista.
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      sessionService.delete(authHeader.substring(7));
    }
    return ResponseEntity.noContent().build();
  }
}
