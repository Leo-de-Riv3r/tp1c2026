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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final SecretKey jwtSecretKey;
  private final long jwtExpirationMs;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserRepository userRepository,
                     @Value("${jwt.secret:}") String jwtSecret,
                     @Value("${jwt.expiration}") long jwtExpirationMs) {
    this.userRepository = userRepository;

    if (jwtSecret == null || jwtSecret.length() < 32) {
      throw new IllegalStateException(
          """
          JWT_SECRET is not configured or is too short.

          Create a .env file at the project root with:
            JWT_SECRET=a-key-at-least-32-characters-long

          Or export it as an environment variable on your system.
          """);
    }

    this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    this.jwtExpirationMs = jwtExpirationMs;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  public UserDto register(RegisterDTO dto) {
    String email = dto.getEmail().trim().toLowerCase();

    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("Email is already registered");
    }

    User user = new User();
    user.setName(dto.getName().trim());
    user.setEmail(email);
    user.setAvatarId(dto.getAvatarId());
    user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
    user.setRole(UserRole.USER);

    User saved = userRepository.save(user);
    return UserDto.from(saved);
  }

  /**
   * Single login endpoint. Finds the user by email in Mongo, validates the password and builds the JWT with the actual User role as a claim.
    * Admin and User share the same flow — the difference lives only in {@link User#getRole()} (seeded as {@link UserRole#ADMIN} in {@code seed.js})
   */
  public LoginResponseDto login(LoginDTO dto) {
    if (dto == null || isBlank(dto.getEmail()) || isBlank(dto.getPassword())) {
      throw new BadInputException("Email and password are required");
    }
    String email = dto.getEmail().trim().toLowerCase();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
    if (user.getPasswordHash() == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid credentials");
    }
    user.setLastLogin(LocalDateTime.now());
    userRepository.save(user);
    UserRole role = user.getRole() == null ? UserRole.USER : user.getRole();
    return new LoginResponseDto(generateJwt(user.getId(), user.getEmail(), role.name()), UserDto.from(user));
  }

  public boolean isTokenValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public String extractUserId(String token) {
    try {
      String subject = parseClaims(token).getSubject();
      if (subject == null || subject.trim().isEmpty()) {
        throw new UnauthorizedException("Invalid token");
      }
      return subject;
    } catch (JwtException e) {
      throw new UnauthorizedException("Invalid token");
    }
  }

  public String extractRole(String token) {
    try {
      String role = parseClaims(token).get("role", String.class);
      return isBlank(role) ? "USER" : role;
    } catch (JwtException e) {
      throw new UnauthorizedException("Invalid token");
    }
  }

  private String generateJwt(String userId, String email, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtExpirationMs);
    return Jwts.builder()
        .subject(userId)
        .claim("email", email)
        .claim("role", role)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(jwtSecretKey)
        .compact();
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(jwtSecretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
