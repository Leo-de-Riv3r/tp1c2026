package com.tacs.tp1c2026.config;

import com.tacs.tp1c2026.entities.dto.common.ApiError;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escribe un body {@link ApiError} en la {@link HttpServletResponse} desde filters / interceptors,
 * donde {@link com.tacs.tp1c2026.exceptions.GlobalExceptionAdvice} no aplica porque el rechazo
 * sucede antes de llegar al handler.
 */
@Component
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
