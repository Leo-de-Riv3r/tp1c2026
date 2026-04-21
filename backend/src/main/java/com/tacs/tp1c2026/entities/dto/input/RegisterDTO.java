package com.tacs.tp1c2026.entities.dto.input;

import lombok.Data;

@Data
// DTO de entrada para registro de usuario.
public class RegisterDTO {
    private String name;
    private String email;
    private String password;
    private String avatarId;
}

