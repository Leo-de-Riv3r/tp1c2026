package com.tacs.tp1c2026.entities.dto.input;

import java.time.LocalDateTime;

public record UserDto(
    String name,
    LocalDateTime registrationDate
) {}
