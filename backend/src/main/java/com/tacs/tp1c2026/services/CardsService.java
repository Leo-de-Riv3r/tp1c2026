package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Card;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.CardsRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CardsService {

    private final CardsRepository cardsRepository;

    public CardsService(CardsRepository cardsRepository) {
        this.cardsRepository = cardsRepository;
    }

    public List<Card> getCatalog() {
        return cardsRepository.findAll();
    }

    // Para obtener el detalle desde el FE o bien para llenar los combos de selección desde el perfil cuando
    // el usuario quiera agregar faltantes o figuritas a su colección, etc
    public Card getById(String id) {
        return cardsRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Card no encontrada: " + id));
    }

    /*
        Por ahora mockeado en el front, pero la idea es que busque si hay figuritas disponibles
        según los filtros en publicaciones y subastas activas
        Los resultados se agrupan por figuritaId, es decir devuelve una entrada por card
        y dentro una lista de publicaciones y subastas activas que la tienen disponible
        * Parámetros opcionales (todos combinables):
        * @param number      número exacto de la card
        * @param description texto parcial en la descripción (jugador, nombre especial, etc.)
        * @param country     selección (ej: "Argentina")
        * @param category    categoría (ej: "LEGENDARIO")
        * @param type        tipo (ej: "JUGADOR")
        TODO: implementar cuando PublicationsService y AuctionsService estén migrados a mongo
        Consultar publicaciones activas + subastas activas, filtrar por los parámetros
        usando los campos desnormalizados (number, description, country, category, type)
        y agrupar por figuritaId antes de devolver.
        hay que cambiar el tipo de return tmb por un modelito nuevo
    */
    public Void searchAvailable(Integer number, String description,
                                           String country, String category, String type) {
        throw new UnsupportedOperationException("searchAvailable: pendiente de implementación");
    }
}
