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
 * Checks the {@link RequiresOwnerOrAdmin} annotation on the handler method.
 *
 * <p>If present: extracts the identifier (from path variable or query param according to
 * {@link RequiresOwnerOrAdmin#source()}) and compares it with the JWT {@code userId} (set by
 * {@link JwtAuthenticationFilter}). Bypass if the JWT role is {@code ADMIN}.
 *
 * <p>Path-variable mode: the variable is required; absent → 403 (configuration error).
 *
 * <p>Query-param mode: if the param is absent, the check bypasses (meaning "default to current
 * user"). If present and does not match → 403.
 *
 * <p>No match and not ADMIN → 403 Forbidden with body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
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

        String resourceUserId;
        if (required.source() == RequiresOwnerOrAdmin.Source.QUERY) {
            resourceUserId = request.getParameter(required.value());
            // Query param absent → bypass (the controller defaults to the JWT user)
            if (resourceUserId == null) {
                return true;
            }
        } else {
            resourceUserId = extractPathVariable(request, required.value());
            if (resourceUserId == null) {
                // Configuration error: the endpoint declares @RequiresOwnerOrAdmin but does not have the expected path variable
                log.error("@RequiresOwnerOrAdmin on {} expects path variable '{}' which is not present", hm.getMethod(), required.value());
                errorWriter.write(response, HttpStatus.FORBIDDEN, "Not authorized");
                return false;
            }
        }

        String jwtUserId = (String) request.getAttribute("userId");
        if (!resourceUserId.equals(jwtUserId)) {
            log.warn("Cross-user access denied: caller userId={} attempted to access resource of userId={}", jwtUserId, resourceUserId);
            errorWriter.write(response, HttpStatus.FORBIDDEN, "You cannot access another user's resources");
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
