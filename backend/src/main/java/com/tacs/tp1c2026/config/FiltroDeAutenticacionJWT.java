package com.tacs.tp1c2026.config;

import com.tacs.tp1c2026.services.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro HTTP que valida JWT en headers de autorización.
 * Extrae userId del token y lo disponibiliza en request attributes.
 */
@Component
public class FiltroDeAutenticacionJWT extends OncePerRequestFilter {

  private final AuthService authService;

  public FiltroDeAutenticacionJWT(AuthService authService) {
    this.authService = authService;
  }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // 1. Si es endpoint público → dejar pasar
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        // 2. Si no hay token → bloquear
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            String token = authorizationHeader.substring(7);

            // 3. Si token inválido → bloquear
            if (!authService.esTokenValido(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 4. Token válido → extraer userId
            Integer userId = authService.extraerUserIdDelToken(token);

            // 5. Guardarlo en request
            request.setAttribute("userId", userId);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 6. Continuar request
        filterChain.doFilter(request, response);
    }

//   Define rutas públicas que no requieren autenticación JWT.
  private boolean isPublicEndpoint(String path) {
    return path.startsWith("/api/auth/")
        || path.startsWith("/h2-console/")
        || path.startsWith("/actuator/");
  }
}

