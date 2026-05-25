package com.tacs.tp1c2026.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chequea la anotación {@link RequiresRole} en el handler method y, si está presente, valida que el role del request attribute (puesto por {@link JwtAuthenticationFilter}) matchee
 * No matchea → 403 Forbidden
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod hm)) {
      return true;
    }
    RequiresRole required = hm.getMethodAnnotation(RequiresRole.class);
    if (required == null) {
      return true;
    }
    Object role = request.getAttribute("role");
    if (role == null || !required.value().equals(role.toString())) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }
    return true;
  }
}
