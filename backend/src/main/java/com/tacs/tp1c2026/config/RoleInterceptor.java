package com.tacs.tp1c2026.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Verifica la anotación {@link RequiresRole} en el handler del request y, si está presente, valida
 * que el rol del atributo del request (seteado por {@link JwtAuthenticationFilter}) coincida.
 * Si no matchea → 403 Forbidden con body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

  private final ApiErrorResponseWriter errorWriter;

  public RoleInterceptor(ApiErrorResponseWriter errorWriter) {
    this.errorWriter = errorWriter;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
    if (!(handler instanceof HandlerMethod hm)) {
      return true;
    }
    RequiresRole required = hm.getMethodAnnotation(RequiresRole.class);
    if (required == null) {
      return true;
    }
    Object role = request.getAttribute("role");
    if (role == null || !required.value().equals(role.toString())) {
      errorWriter.write(response, HttpStatus.FORBIDDEN, "No tenés permiso para acceder a este recurso");
      return false;
    }
    return true;
  }
}
