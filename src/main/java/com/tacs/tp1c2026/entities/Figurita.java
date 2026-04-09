package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.Categoria;
import lombok.Getter;

@Getter
public class Figurita {
    private long id;
    private Integer numero;
    private String jugador;
    private String seleccion;
    private String equipo;
    private Categoria categoria;
}
