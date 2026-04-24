package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.repositories.FiguritasRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FiguritasService {

    private final FiguritasRepository figuritasRepository;

    public FiguritasService(FiguritasRepository figuritasRepository) {
        this.figuritasRepository = figuritasRepository;
    }

    public List<Figurita> getCatalog() {
        return figuritasRepository.findAll();
    }

    // Para obtener el detalle desde el FE o bien para llenar los combos de selección desde el perfil cuando
    // el usuario quiera agregar faltantes o figuritas a su colección, etc
    public Figurita getById(Integer id) {
        return figuritasRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Figurita no encontrada: " + id));
    }

    /*
        Por ahora mockeado en el front, pero la idea es que busque si hay figuritas disponibles
        según los filtros en publicaciones y subastas activas
        Los resultados se agrupan por figuritaId, es decir devuelve una entrada por figurita
        y dentro una lista de publicaciones y subastas activas que la tienen disponible
        * Parámetros opcionales (todos combinables):
        * @param number      número exacto de la figurita
        * @param description texto parcial en la descripción (jugador, nombre especial, etc.)
        * @param country     selección (ej: "Argentina")
        * @param category    categoría (ej: "LEGENDARIO")
        * @param type        tipo (ej: "JUGADOR")
        TODO: implementar cuando PublicacionesService y SubastasService estén migrados a mongo
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
