package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.Categoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "figuritas")
public class Figurita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "jugador", nullable = false)
    private String jugador;

    @Column(name="origen", nullable = false)
    private String origen;

    public Figurita() {}

    public Figurita(Integer numero, String jugador, String origen) {
        this.numero = numero;
        this.jugador = jugador;
        this.origen = origen;
    }

}
