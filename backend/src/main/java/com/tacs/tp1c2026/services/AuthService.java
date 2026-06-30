package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.dto.user.input.LoginDTO;
import com.tacs.tp1c2026.entities.dto.user.input.RegisterDTO;
import com.tacs.tp1c2026.entities.dto.user.output.LoginResponseDto;
import com.tacs.tp1c2026.entities.dto.user.output.UserDto;
import com.tacs.tp1c2026.entities.enums.UserRole;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.exceptions.BadInputException;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.UnauthorizedException;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final SessionService sessionService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserRepository userRepository, SessionService sessionService) {
    this.userRepository = userRepository;
    this.sessionService = sessionService;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  /**
   * Registra un nuevo usuario y devuelve una sesión lista para usar (token + UserDto), para que el FE pueda
   * auto-loguear sin un segundo round-trip.
   */
  public LoginResponseDto register(RegisterDTO dto) {
    String email = dto.getEmail().trim().toLowerCase();

    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("El email ya está registrado");
    }

    User user = new User();
    user.setName(dto.getName().trim());
    user.setEmail(email);
    user.setAvatarId(dto.getAvatarId());
    user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
    user.setRole(UserRole.USER);

    User saved = userRepository.save(user);
    return new LoginResponseDto(
        sessionService.create(saved.getId(), saved.getRole().name()),
        UserDto.from(saved)
    );
  }

  /**
   * Endpoint único de login. Busca el usuario por email en Mongo, valida la contraseña y crea una
   * sesión del lado del servidor que lleva el rol real del User.
   * Admin y User comparten el mismo flujo — la diferencia vive solo en {@link User#getRole()} (sembrado como {@link UserRole#ADMIN} en {@code seed.js})
   */
  public LoginResponseDto login(LoginDTO dto) {
    if (dto == null || isBlank(dto.getEmail()) || isBlank(dto.getPassword())) {
      throw new BadInputException("El email y la contraseña son obligatorios");
    }
    String email = dto.getEmail().trim().toLowerCase();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));
    if (user.getPasswordHash() == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Credenciales inválidas");
    }
    // No tocamos lastLogin acá: el load test detectó que dos logins concurrentes del mismo user
    // chocaban en el @Version del User al hacer save() y agotaban los 3 retries del @Retryable
    // → 409 Conflict. El campo no se usa en ningún endpoint, así que lo más limpio es borrarlo.
    UserRole role = user.getRole() == null ? UserRole.USER : user.getRole();
    return new LoginResponseDto(sessionService.create(user.getId(), role.name()), UserDto.from(user));
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
