package com.tacs.tp1c2026.unit.mothers;

import com.tacs.tp1c2026.entities.FiguritaColeccion;
import com.tacs.tp1c2026.entities.PublicacionIntercambio;
import com.tacs.tp1c2026.entities.Usuario;

public class PublicacionIntercambioMother {

    private Usuario publicante;
    private FiguritaColeccion figuritaColeccion;

    private PublicacionIntercambioMother() {
        this.publicante = UsuarioMother.builder().build();
        this.figuritaColeccion = FiguritaColeccionMother.builder().build();
    }

    public static PublicacionIntercambioMother builder() {
        return new PublicacionIntercambioMother();
    }

    public PublicacionIntercambioMother withPublicante(Usuario publicante) {
        this.publicante = publicante;
        return this;
    }

    public PublicacionIntercambioMother withFiguritaColeccion(FiguritaColeccion figuritaColeccion) {
        this.figuritaColeccion = figuritaColeccion;
        return this;
    }

    public PublicacionIntercambio build() {
        PublicacionIntercambio publicacion = new PublicacionIntercambio();
        publicacion.setPublicante(this.publicante);
        publicacion.setFiguritaColeccion(this.figuritaColeccion);
        return publicacion;
    }
}
