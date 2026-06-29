package com.tacs.tp1c2026.config;

import com.tacs.tp1c2026.entities.session.Session;
import com.tacs.tp1c2026.repositories.UserRepository;
import com.tacs.tp1c2026.services.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro HTTP que valida la sesión del header {@code Authorization} (token opaco = sessionId).
 * Si la sesión es válida la "toca" (sliding) y deja {@code userId} y {@code role} como atributos
 * del request para que los controllers los recuperen con {@code @RequestAttribute}.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

  private final SessionService sessionService;
  private final ApiErrorResponseWriter errorWriter;
  private final UserRepository userRepository;

  public SessionAuthenticationFilter(SessionService sessionService, ApiErrorResponseWriter errorWriter, UserRepository userRepository) {
    this.sessionService = sessionService;
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
      Optional<Session> sessionOpt = sessionService.validateAndTouch(token);
      if (sessionOpt.isEmpty()) {
        errorWriter.write(response, HttpStatus.UNAUTHORIZED, "Sesión inválida o expirada");
        return;
      }
      Session session = sessionOpt.get();
      if (!userRepository.existsById(session.getUserId())) {
        sessionService.delete(token);
        errorWriter.write(response, HttpStatus.UNAUTHORIZED, "El usuario de la sesión ya no existe");
        return;
      }
      request.setAttribute("userId", session.getUserId());
      request.setAttribute("role", session.getRole());
    } catch (Exception e) {
      errorWriter.write(response, HttpStatus.UNAUTHORIZED, "Sesión inválida o expirada");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isPublicEndpoint(String path) {
    return path.startsWith("/api/auth")
        || path.startsWith("/actuator");
  }
}
