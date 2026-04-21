package com.tacs.tp1c2026.entities.dto.output;

import lombok.Data;

@Data
public class LoginResponseDto {
  private String token;
  private UserDto user;
}

