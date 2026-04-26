package com.tacs.tp1c2026.entities.dto.user.input;

import lombok.Data;

import java.time.LocalDateTime;
//BORRAR ESTE ARCHIVO DSP
@Data
public class UserDto{
    private String nombre;
    private LocalDateTime fechaAlta;
}

