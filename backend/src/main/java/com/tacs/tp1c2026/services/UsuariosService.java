package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.Usuario;
import com.tacs.tp1c2026.entities.embedded.FiguritaColeccion;
import com.tacs.tp1c2026.entities.embedded.FiguritaFaltante;
import com.tacs.tp1c2026.exceptions.UserNotFoundException;
import com.tacs.tp1c2026.repositories.UsuariosRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UsuariosService {

    private final UsuariosRepository usuariosRepository;
    private final FiguritasService figuritasService;

    public UsuariosService(UsuariosRepository usuariosRepository,
                           FiguritasService figuritasService) {
        this.usuariosRepository = usuariosRepository;
        this.figuritasService = figuritasService;
    }

    public List<Usuario> listarUsuarios() {
        return usuariosRepository.findAll();
    }

    public Usuario obtenerUsuario(String userId) {
        return usuariosRepository
            .findById(userId)
            .orElseThrow(() -> new UserNotFoundException("No se encontró al usuario con id: " + userId));
    }

    /* Métodos sobre la colección del usuario */

    public List<FiguritaColeccion> obtenerColeccion(String userId) {
        return obtenerUsuario(userId).getCollection();
    }

    /*
        Agrega una figurita a la colección del usuario.
        Si ya existe, incrementa la cantidad, sino, crea el subdoc con los datos del catálogo
        figuritaId -> id de la figurita en el catálogo (x ej: "fig_042")
        devuelve el subdocumento actualizado
    */
    public FiguritaColeccion agregarAColeccion(String userId, String figuritaId) {
        Figurita figurita = figuritasService.getById(figuritaId);
        long updated = usuariosRepository.incrementCollectionItemQuantity(userId, figuritaId);
        if (updated == 0) {
            usuariosRepository.pushToCollection(userId, FiguritaColeccion.fromCatalog(figurita));
        }
        return obtenerUsuario(userId).getCollection().stream()
            .filter(f -> f.getFiguritaId().equals(figuritaId))
            .findFirst()
            .orElseThrow();
    }

    /* 
        baja en 1 la cantidad de una figurita en la colección, si llegase a 0, elimina el subdoc de la coleccion del usuario
        figuritaId -> id de la figurita en el catálogo
    */
    public void decrementFromCollection(String userId, String figuritaId) {
        long updated = usuariosRepository.decrementCollectionQuantity(userId, figuritaId);
        if (updated == 0) {
            usuariosRepository.pullFromCollection(userId, figuritaId);
        }
    }

    /* Métodos sobre los faltantes del usuario */

    public List<FiguritaFaltante> obtenerFaltantes(String userId) {
        return obtenerUsuario(userId).getMissingCards();
    }

    /*
        agrega una figurita faltante al usuario, si ya la tenia en la lista, no hace nada
        figuritaId -> id de la figurita en el catálogo (x ej: "fig_042")
        devuelve el subdocumento actualizado
    */
    public FiguritaFaltante agregarFaltante(String userId, String figuritaId) {
        Figurita figurita = figuritasService.getById(figuritaId);
        FiguritaFaltante ff = FiguritaFaltante.fromCatalog(figurita);
        usuariosRepository.pushToMissingCards(userId, figuritaId, ff);
        return ff;
    }

    /*
        Elimina una figurita de la lista missingCards del usuario, ya sea porque la consiguió en una subasta/publicación que finalizó en intercambio
        o bien ya no le interesa esa figurita/la consiguió de forma manual
        figuritaid es el id en el catalogo
    */
    public void removeFromMissingCards(String userId, String figuritaId) {
        usuariosRepository.pullFromMissingCards(userId, figuritaId);
    }

    /* Sugerencias */

    /*
        para hacer cuando esté implementado el publicacionesRepository
        el cron GeneradorSugerencias calcula y guarda en suggestionsIds del usuario los ids de las
        publicaciones o subastas sugeridas, cuando los pide el FE, se busca en publicacionesrepository/subastas
    */
    // public List<Publicacion> obtenerSugerencias(String userId) {
    //     List<String> ids = obtenerUsuario(userId).getSuggestionsIds();
    //     return publicacionesRepository.findAllById(ids); // busca por los IDs guardados
    // }
}