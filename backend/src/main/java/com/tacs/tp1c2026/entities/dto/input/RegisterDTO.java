package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
// DTO de entrada para registro de usuario.
public class RegisterDTO {
  @NotBlank(message = "El nombre es obligatorio")
  private String name;
  @NotBlank(message = "El email es obligatorio")
  private String email;
  @NotBlank(message = "La contraseña es obligatoria")
  private String password;
  @NotBlank(message = "El id de avatar es obligatorio")
  private String avatarId;
}

