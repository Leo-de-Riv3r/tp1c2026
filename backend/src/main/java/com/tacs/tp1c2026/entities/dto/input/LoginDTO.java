package com.tacs.tp1c2026.entities.dto.input;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
  @NotBlank(message = "El email es requerido")
  private String email;
  @NotBlank(message = "La contraseña es requerida")
  private String password;
}

