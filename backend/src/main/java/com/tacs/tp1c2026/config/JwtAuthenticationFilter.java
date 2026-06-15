package com.tacs.tp1c2026.config;

import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.services.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro HTTP que valida el JWT del header {@code Authorization}. Si el token es válido,
 * deja {@code userId} y {@code role} como atributos del request para que los controllers
 * los recuperen con {@code @RequestAttribute}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final AuthService authService;
  private final ApiErrorResponseWriter errorWriter;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(AuthService authService, ApiErrorResponseWriter errorWriter, UserRepository userRepository) {
    this.authService = authService;
    this.errorWriter = errorWriter;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    String path = request.getRequestURI();

    if (isPublicEndpoint(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      errorWriter.write(response, HttpStatus.UNAUTHORIZED, "No se provee un token de autenticación");
      return;
    }

    try {
      String token = header.substring(7);
      if (!authService.isTokenValid(token)) {
        errorWriter.write(response, HttpStatus.UNAUTHORIZED, "Token de autenticación inválido");
        return;
      }
      String userId = authService.extractUserId(token);
      if (!userRepository.existsById(userId)) {
        errorWriter.write(response, HttpStatus.UNAUTHORIZED, "El usuario del token ya no existe");
        return;
      }
      request.setAttribute("userId", userId);
      request.setAttribute("role", authService.extractRole(token));
    } catch (Exception e) {
      errorWriter.write(response, HttpStatus.UNAUTHORIZED, "Token de autenticación inválido");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isPublicEndpoint(String path) {
    return path.startsWith("/api/auth")
        || path.startsWith("/actuator");
  }
}
