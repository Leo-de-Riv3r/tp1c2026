package com.tacs.tp1c2026.entities.exchange.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Embedded feedback dejado por una de las partes de un intercambio sobre la otra.
 * Vive en los slots {@code feedbackFromA} / {@code feedbackFromB} de un {@link com.tacs.tp1c2026.entities.exchange.Exchange}.
 * El reviewer queda implícito por el slot en el que vive (no se almacena explícitamente).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    private Integer score;
    private String comment;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
