package com.tacs.tp1c2026.entities.dto.output;

import lombok.Data;

@Data
public class AuthResponseDto {
  private String token;
  private UserDto user;
}

