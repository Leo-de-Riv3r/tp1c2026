package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.dto.user.input.LoginDTO;
import com.tacs.tp1c2026.entities.dto.user.input.RegisterDTO;
import com.tacs.tp1c2026.entities.dto.user.output.LoginResponseDto;
import com.tacs.tp1c2026.entities.dto.user.output.UserDto;
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
  private final String adminEmail;
  private final String adminPassword;
  private final String adminPasswordHash;

  public AuthService(UserRepository userRepository,
                     @Value("${jwt.secret}") String jwtSecret,
                     @Value("${jwt.expiration}") long jwtExpirationMs,
                     @Value("${admin.email:admin@tacs.local}") String adminEmail,
                     @Value("${admin.password:}") String adminPassword,
                     @Value("${admin.password-hash:}") String adminPasswordHash) {
    this.userRepository = userRepository;
    this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    this.jwtExpirationMs = jwtExpirationMs;
    this.passwordEncoder = new BCryptPasswordEncoder();
    this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
    this.adminPassword = adminPassword;
    this.adminPasswordHash = adminPasswordHash;
  }

  public UserDto register(RegisterDTO dto) {
    String email = dto.getEmail().trim().toLowerCase();

    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("El email ya se encuentra registrado");
    }

    User user = new User();
    user.setName(dto.getName().trim());
    user.setEmail(email);
    user.setAvatarId(dto.getAvatarId());
    user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

    User saved = userRepository.save(user);
    return UserDto.from(saved);
  }

  public LoginResponseDto login(LoginDTO dto) {
    if (dto == null || isBlank(dto.getEmail()) || isBlank(dto.getPassword())) {
      throw new BadInputException("Email y password son obligatorios");
    }

    String email = dto.getEmail().trim().toLowerCase();
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

    if (user.getPasswordHash() == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Credenciales inválidas");
    }

    user.setLastLogin(LocalDateTime.now());
    userRepository.save(user);

    return new LoginResponseDto(generateJwt(user.getId(), user.getEmail(), "USER"), UserDto.from(user));
  }

  public LoginResponseDto adminLogin(LoginDTO dto) {
    if (dto == null || isBlank(dto.getEmail()) || isBlank(dto.getPassword())) {
      throw new BadInputException("Email y password son obligatorios");
    }
    if (isBlank(adminPasswordHash) && isBlank(adminPassword)) {
      throw new UnauthorizedException("Admin no configurado");
    }

    String email = dto.getEmail().trim().toLowerCase();
    boolean validPassword = !isBlank(adminPasswordHash)
        ? passwordEncoder.matches(dto.getPassword(), adminPasswordHash)
        : dto.getPassword().equals(adminPassword);

    if (!email.equals(adminEmail) || !validPassword) {
      throw new UnauthorizedException("Credenciales inválidas");
    }

    UserDto admin = new UserDto("admin", "Administrador", email, null, 0, "admin", LocalDateTime.now().toString());
    return new LoginResponseDto(generateJwt("admin", email, "ADMIN"), admin);
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
        throw new UnauthorizedException("Token inválido");
      }
      return subject;
    } catch (JwtException e) {
      throw new UnauthorizedException("Token inválido");
    }
  }

  public String extractRole(String token) {
    try {
      String role = parseClaims(token).get("role", String.class);
      return isBlank(role) ? "USER" : role;
    } catch (JwtException e) {
      throw new UnauthorizedException("Token inválido");
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
