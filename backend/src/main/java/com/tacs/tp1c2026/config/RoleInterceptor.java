package com.tacs.tp1c2026.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Checks the {@link RequiresRole} annotation on the handler method and, if present, validates that the role from the request attribute (set by {@link JwtAuthenticationFilter}) matches.
 * No match → 403 Forbidden with body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}
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
      errorWriter.write(response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
      return false;
    }
    return true;
  }
}
