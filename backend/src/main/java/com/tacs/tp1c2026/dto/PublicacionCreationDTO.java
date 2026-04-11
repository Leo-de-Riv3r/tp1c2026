package com.tacs.tp1c2026.dto;

import com.tacs.tp1c2026.entities.enums.ModoIntercambio;

public record PublicacionCreationDTO(FiguritaCreationDTO figurita, Integer disponibilidad, ModoIntercambio modoIntercambio) {
}
