package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.dto.CardDTO;
import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.repository.FiguritaRepository;

public class CardService {

    private final FiguritaRepository figuritaRepository;

    public CardService(FiguritaRepository figuritaRepository) {
        this.figuritaRepository = figuritaRepository;
    }

    public Figurita createCard(CardDTO cardDTO) {
        Figurita newFigurita = new Figurita();
        return figuritaRepository.save(newFigurita);
    }

}
