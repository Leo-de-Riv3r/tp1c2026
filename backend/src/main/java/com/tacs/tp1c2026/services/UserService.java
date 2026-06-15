package com.tacs.tp1c2026.services;

import com.tacs.tp1c2026.entities.card.Card;
import com.tacs.tp1c2026.entities.enums.UserRole;
import com.tacs.tp1c2026.entities.user.User;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;
import com.tacs.tp1c2026.entities.user.embedded.Suggestion;
import com.tacs.tp1c2026.exceptions.ConflictException;
import com.tacs.tp1c2026.exceptions.InsufficientCardException;
import com.tacs.tp1c2026.exceptions.MissingCardException;
import com.tacs.tp1c2026.exceptions.NotFoundException;
import com.tacs.tp1c2026.entities.dto.user.output.CollectionCardResult;
import com.tacs.tp1c2026.repositories.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CardService cardService;

    public UserService(UserRepository userRepository,
                       CardService cardService) {
        this.userRepository = userRepository;
        this.cardService = cardService;
    }

    /**
     * Devuelve todos los users regulares registrados (excluyendo {@link UserRole#ADMIN}).
     * El admin es un User en Mongo, pero no debería aparecer en listas de candidatos
     * para intercambio, sugerencias ni listados.
     */
    public List<User> getAll() {
        return userRepository.findAll().stream()
            .filter(u -> u.getRole() != UserRole.ADMIN)
            .toList();
    }

    /**
     * Devuelve un user por su ID, o lanza excepción si no existe.
     * @param userId ID del user en Mongo
     * @return la entidad {@link User}
     * @throws NotFoundException si no existe ningún user con ese ID
     */
    public User getById(String userId) throws NotFoundException {
        return userRepository
            .findById(userId)
            .orElseThrow(() -> new NotFoundException("No se encontró el user con id: " + userId));
    }

    /* Collection */

    /**
     * Devuelve la colección de figuritas de un user.
     * @param userId ID del user
     * @return lista de entradas {@link CollectionCard}
     */
    public List<CollectionCard> getUserCardCollection(String userId) throws NotFoundException {
        return getById(userId).getCollection();
    }

    /**
     * Agrega una figurita a la colección del user.
     * Si la figurita ya está, incrementa su cantidad.
     * Si no, crea una entrada nueva usando los datos del catálogo.
     * @param userId ID del user
     * @param cardId ID de la figurita en el catálogo (ej. "FWC1")
     * @return la entrada {@link CollectionCard} actualizada envuelta en {@link CollectionCardResult}
     */
    public CollectionCardResult addCardToUserCollection(String userId, String cardId) throws NotFoundException, NotFoundException {
        Card card = cardService.getById(cardId);
        User user = getById(userId);
        boolean created = !user.hasInCollection(cardId);
        user.addToCollection(CollectionCard.fromCatalog(card));
        userRepository.save(user);
        CollectionCard saved = user.findCollectionItem(cardId)
            .orElseThrow(() -> new NotFoundException("La figurita no quedó en la colección después del add"));
        return new CollectionCardResult(saved, created);
    }

    /**
     * Decrementa en uno la cantidad de una figurita en la colección del user.
     * Si la cantidad llega a cero, elimina la entrada por completo.
     * @param userId ID del user
     * @param cardId ID de la figurita en el catálogo
     */
    public void decrementFromCollection(String userId, String cardId) throws InsufficientCardException, MissingCardException, NotFoundException, NotFoundException {
        // Valida que la figurita exista en el catálogo
        cardService.getById(cardId);
        User user = getById(userId);
        user.removeFromCollection(cardId, 1);
        userRepository.save(user);
    }

    /* Missing cards */

    /**
     * Devuelve la lista de figuritas que el user está buscando.
     * @param userId ID del user
     * @return lista de entradas {@link MissingCard}
     */
    public List<MissingCard> getUserMissingCards(String userId) throws NotFoundException {
        return getById(userId).getMissingCards();
    }

    /**
     * Marca una figurita como faltante para el user.
     * Si la figurita ya está en la lista, no hace nada.
     * @param userId ID del user
     * @param cardId ID de la figurita en el catálogo (ej. "FWC1")
     * @return la {@link MissingCard} agregada (o la que ya existía)
     */
    public MissingCard addMissingCard(String userId, String cardId) throws NotFoundException, NotFoundException {
        Card card = cardService.getById(cardId);
        User user = getById(userId);
        if (user.hasInCollection(cardId)) {
            throw new ConflictException("La figurita #" + card.getNumber() + " (" + card.getDescription() + ") ya está en tu colección");
        }
        MissingCard mc = MissingCard.fromCatalog(card);
        user.addToMissingCards(mc);
        userRepository.save(user);
        return mc;
    }

    /**
     * Saca una figurita de la lista de faltantes del user.
     * Se llama cuando el user obtiene la figurita por una subasta, intercambio o manualmente.
     * @param userId ID del user
     * @param cardId ID de la figurita en el catálogo
     */
    public void removeFromMissingCards(String userId, String cardId) throws NotFoundException, NotFoundException {
        cardService.getById(cardId);
        User user = getById(userId);
        user.removeFromMissingCards(cardId);
        userRepository.save(user);
    }

    /* Suggestions */

    /**
     * Devuelve las sugerencias persistidas para un user.
     * @param userId ID del user
     * @return lista de {@link Suggestion}
     */
    public List<Suggestion> getUserSuggestions(String userId) throws NotFoundException {
        return getById(userId).getSuggestions();
    }

    public void saveAll(List<User> users) {
        userRepository.saveAll(users);
    }

}
