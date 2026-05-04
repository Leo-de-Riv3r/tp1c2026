package com.tacs.tp1c2026.entities.dto.user.output;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {
  private String token;
  private UserDto user;
}
