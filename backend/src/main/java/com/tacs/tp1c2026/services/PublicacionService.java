package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.dto.PublicacionCreationDTO;
import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.Publicacion;
import com.tacs.tp1c2026.repository.PublicacionRepository;
import com.tacs.tp1c2026.repository.FiguritaRepository;

public class PublicacionService {

    private final FiguritaRepository figuritaRepository;
    private final PublicacionRepository publicacionRepository;

    public PublicacionService(FiguritaRepository figuritaRepository, PublicacionRepository publicacionRepository) {
        this.figuritaRepository = figuritaRepository;
        this.publicacionRepository = publicacionRepository;
    }

    public Publicacion createPublicacion(PublicacionCreationDTO publicacionCreationDTO) {
        Figurita newFigurita = new Figurita(
                publicacionCreationDTO.figurita().numero(),
                publicacionCreationDTO.figurita().jugador(),
                publicacionCreationDTO.figurita().origen()
        );
        Publicacion collecion = new Publicacion(
                newFigurita,
                publicacionCreationDTO.disponibilidad(),
                publicacionCreationDTO.modoIntercambio()
        );
        return publicacionRepository.save(collecion);
    }

}
