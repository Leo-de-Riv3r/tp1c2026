package com.tacs.tp1c2026.config;

import com.tacs.tp1c2026.entities.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Map;

/**
 * Chequea la anotación {@link RequiresOwnerOrAdmin} en el handler method
 * Si está presente: extrae el path variable indicado por {@link RequiresOwnerOrAdmin#value()}, lo compara con el {@code userId} del JWT (puesto por {@link JwtAuthenticationFilter}) y permite el acceso si matchean. Bypass si el role del JWT es {@code ADMIN}
 * No matchea ni es ADMIN → 403 Forbidden con body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}
 */
@Component
public class OwnerOrAdminInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(OwnerOrAdminInterceptor.class);

    private final ApiErrorResponseWriter errorWriter;

    public OwnerOrAdminInterceptor(ApiErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        RequiresOwnerOrAdmin required = hm.getMethodAnnotation(RequiresOwnerOrAdmin.class);
        if (required == null) {
            return true;
        }

        String role = (String) request.getAttribute("role");
        if (UserRole.ADMIN.name().equals(role)) {
            return true;
        }

        String pathUserId = extractPathVariable(request, required.value());
        if (pathUserId == null) {
            // Error de configuración: el endpoint declara @RequiresOwnerOrAdmin pero no tiene el path variable esperado
            log.error("@RequiresOwnerOrAdmin on {} expects path variable '{}' which is not present", hm.getMethod(), required.value());
            errorWriter.write(response, HttpStatus.FORBIDDEN, "No autorizado");
            return false;
        }

        String jwtUserId = (String) request.getAttribute("userId");
        if (!pathUserId.equals(jwtUserId)) {
            log.warn("Cross-user access denied: caller userId={} attempted to access resource of userId={}", jwtUserId, pathUserId);
            errorWriter.write(response, HttpStatus.FORBIDDEN, "No podés acceder a recursos de otro usuario");
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private String extractPathVariable(HttpServletRequest request, String name) {
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attr instanceof Map)) {
            return null;
        }
        return ((Map<String, String>) attr).get(name);
    }
}
