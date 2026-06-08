package com.tacs.tp1c2026.config;

import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Map;

/**
 * Checks the {@link ValidatesPathUser} annotation on the handler method.
 * If present, extracts the path variable indicated by {@link ValidatesPathUser#value()}
 * and verifies that a user with that ID exists in the database.
 * If not found → 404 Not Found with body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
 */
@Component
public class UserValidationInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final ApiErrorResponseWriter errorWriter;

    public UserValidationInterceptor(UserService userService, ApiErrorResponseWriter errorWriter) {
        this.userService = userService;
        this.errorWriter = errorWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        ValidatesPathUser annotation = hm.getMethodAnnotation(ValidatesPathUser.class);
        if (annotation == null) {
            return true;
        }

        String userId = extractPathVariable(request, annotation.value());
        if (userId == null) {
            errorWriter.write(response, HttpStatus.BAD_REQUEST, "User ID required in the URL");
            return false;
        }

        try {
            userService.getById(userId);
            return true;
        } catch (NotFoundException e) {
            errorWriter.write(response, HttpStatus.NOT_FOUND, e.getMessage());
            return false;
        }
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
