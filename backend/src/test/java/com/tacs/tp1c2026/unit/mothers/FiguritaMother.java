package com.tacs.tp1c2026.unit.mothers;

import java.util.Random;

import org.springframework.stereotype.Component;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.enums.Categoria;

@Component
public class FiguritaMother {

    private Integer id;
    private Integer numero;
    private String jugador;
    private String descripcion;
    private String seleccion;
    private String equipo;
    private Categoria categoria;

    private FiguritaMother() {
        this.id = new Random().nextInt(1000);
        this.numero = this.id;
        this.jugador = "Jugador" + this.id;
        this.descripcion = "Descripcion " + this.id;
        this.seleccion = "Seleccion" + this.id;
        this.equipo = "Equipo" + this.id;
        this.categoria = Categoria.COMUN;
    }

    public static FiguritaMother builder() {
        return new FiguritaMother();
    }

    public FiguritaMother withId(Integer id) {
        this.id = id;
        return this;
    }

    public FiguritaMother withNumero(Integer numero) {
        this.numero = numero;
        return this;
    }

    public FiguritaMother withJugador(String jugador) {
        this.jugador = jugador;
        return this;
    }

    public FiguritaMother withDescripcion(String descripcion) {
        this.descripcion = descripcion;
        return this;
    }

    public FiguritaMother withSeleccion(String seleccion) {
        this.seleccion = seleccion;
        return this;
    }

    public FiguritaMother withEquipo(String equipo) {
        this.equipo = equipo;
        return this;
    }

    public FiguritaMother withCategoria(Categoria categoria) {
        this.categoria = categoria;
        return this;
    }

    public Figurita build() {
        Figurita figurita = new Figurita();
        figurita.setId(this.id);
        figurita.setNumero(this.numero);
        figurita.setJugador(this.jugador);
        figurita.setDescripcion(this.descripcion);
        figurita.setSeleccion(this.seleccion);
        figurita.setEquipo(this.equipo);
        figurita.setCategoria(this.categoria);
        return figurita;
    }
}
