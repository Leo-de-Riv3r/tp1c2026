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
 * Verifica la anotación {@link ValidatesPathUser} en el handler del request.
 * Si está presente, extrae el path variable indicado por {@link ValidatesPathUser#value()}
 * y valida que exista un user con ese ID en la BD.
 * Si no se encuentra → 404 Not Found con body {@link com.tacs.tp1c2026.entities.dto.common.ApiError}.
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
            errorWriter.write(response, HttpStatus.BAD_REQUEST, "Se requiere el ID del usuario en la URL");
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
