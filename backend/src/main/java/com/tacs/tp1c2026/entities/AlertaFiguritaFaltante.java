package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("FIGURITA_FALTANTE")
public class AlertaFiguritaFaltante extends Alerta {

    @ManyToOne
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "figurita_id", referencedColumnName = "id")
    private Figurita figurita;

    protected AlertaFiguritaFaltante() {}

    public AlertaFiguritaFaltante(Usuario usuario, Figurita figurita) {
        this.usuario = usuario;
        this.figurita = figurita;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Figurita getFigurita() {
        return figurita;
    }

    @Override
    public AlertaDto visit(AlertaVisitor visitor) {
        return visitor.visit(this);
    }

}