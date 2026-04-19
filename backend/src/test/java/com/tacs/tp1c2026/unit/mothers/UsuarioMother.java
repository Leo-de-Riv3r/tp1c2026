package com.tacs.tp1c2026.unit.mothers;

import java.util.ArrayList;
import java.util.List;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.Usuario;

public class UsuarioMother {

    private Integer id;
    private String nombre;
    private final List<FiguritaColeccion> repetidas = new ArrayList<>();
    private final List<Figurita> faltantes = new ArrayList<>();

    private UsuarioMother() {
        this.id = null;
        this.nombre = "Usuario";
    }

    public static UsuarioMother builder() {
        return new UsuarioMother();
    }

    public UsuarioMother withId(Integer id) {
        this.id = id;
        return this;
    }

    public UsuarioMother withNombre(String nombre) {
        this.nombre = nombre;
        return this;
    }

    public UsuarioMother withRepetida(FiguritaColeccion repetida) {
        if (repetida != null) {
            this.repetidas.add(repetida);
        }
        return this;
    }

    public UsuarioMother withRepetidas(FiguritaColeccion repetida) {
        return withRepetida(repetida);
    }

    public UsuarioMother withRepetida() {
        return withRepetida(FiguritaColeccionMother.builder().build());
    }

    public UsuarioMother withFaltante(Figurita faltante) {
        if (faltante != null) {
            this.faltantes.add(faltante);
        }
        return this;
    }

    public UsuarioMother withFigurita(Figurita faltante) {
        return withFaltante(faltante);
    }

    public UsuarioMother withFigurita() {
        return withFaltante(FiguritaMother.builder().build());
    }

    public Usuario build() {
        Usuario usuario = new Usuario();
        usuario.initializeVectorProfile();
        usuario.setId(this.id);
        usuario.setNombre(this.nombre);

        for (FiguritaColeccion repetida : this.repetidas) {
            usuario.agregarRepetidas(repetida);
        }
        for (Figurita faltante : this.faltantes) {
            usuario.agregarFaltantes(faltante);
        }

        return usuario;
    }

	public UsuarioMother withFaltante() {
        return withFaltante(FiguritaMother.builder().build());
	}
}
