package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.FiguritaYaPublicadaException;
import com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


@TypeAlias("usuario")
@Document(collection = "usuarios")
public class User {

    @Id
    private Integer id;

    @Getter
    private String name;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Integer avatarId;


    private Double rating = null;

    private Integer exchangesCount = 0;

    private LocalDateTime lastLogin;

    private final LocalDateTime creationDate = LocalDateTime.now();

    private final List<StickerCollection> collections = new ArrayList<>();

    private final List<Sticker> missingStickers = new ArrayList<>();

    @DocumentReference
    private final List<User> suggestionsIds = new ArrayList<>();


    private final List<Alert> alerts = new ArrayList<>();

    private Profile profile = new Profile();

    public void addSuggestion(User suggestedUser) {
        this.suggestionsIds.add(suggestedUser);
    }

    public void addRepeatedSticker(StickerCollection collectionSticker) {
        this.collections.add(collectionSticker);
        this.profile.addSticker(collectionSticker);
    }

    public void addMissingSticker(Sticker sticker) {
        this.missingStickers.add(sticker);
        this.profile.addSticker(sticker);
    }

    public StickerCollection getRepeatedByNumber(Integer publishedStickerNumber) throws FiguritaNoEncontradaException {
        return this.collections.stream()
                .filter(repeatedSticker -> repeatedSticker.isOf(publishedStickerNumber))
                .findFirst()
                .orElseThrow(() -> new FiguritaNoEncontradaException("El usuario no posee la figurita " + publishedStickerNumber));
    }

    /**
     * Obtiene las figuritas repetidas indicadas por sus números y valida que estén disponibles para oferta.
     *
     * @param stickerNumbers lista de números de figuritas a obtener
     * @return lista de FiguritaColeccion encontradas y disponibles para oferta
     * @throws FiguritaNoEncontradaException si alguna figurita no se encuentra
     * @throws FiguritaNoDisponibleException si alguna figurita no está disponible para oferta
     */
    public List<StickerCollection> getStickersForOffer(List<Integer> stickerNumbers)
            throws FiguritaNoEncontradaException, FiguritaNoDisponibleException {
        List<StickerCollection> foundStickers = new ArrayList<>();

        for (Integer stickerNumber : stickerNumbers) {
            StickerCollection sticker = getRepeatedByNumber(stickerNumber);
            sticker.increaseOfferedAmount();
            foundStickers.add(sticker);
        }

        return foundStickers;
    }

    /**
     * Crea una publicación de intercambio para una figurita repetida.
     *
     * @param stickerNumber número de la figurita a publicar
     * @return la publicación creada
     * @throws FiguritaNoEncontradaException                                si la figurita no se encuentra
     * @throws com.tacs.tp1c2026.exceptions.FiguritaYaPublicadaException    si ya está publicada
     * @throws com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException si no hay stock
     */
    public TradePublication publishSticker(Integer stickerNumber)
            throws FiguritaNoEncontradaException,
            FiguritaYaPublicadaException,
            FiguritasInsuficientesException {
        StickerCollection sticker = getRepeatedByNumber(stickerNumber);
        return sticker.createPublication(this, stickerNumber);
    }

    public void addAlert(Alert alert) {
        this.alerts.add(alert);
    }

    @PostConstruct
    private void initializeVectorProfile() {
        this.profile = new Profile(this.collections, this.missingStickers);
    }

    public void clearSuggestions() {
        this.suggestionsIds.clear();
    }

    public void addSticker(Sticker sticker, User originUser) {
        StickerCollection collection = this.findCollection(sticker);
        if (collection != null) {
            collection.increaseAmount();
        } else {
            if (missingStickers.contains(sticker)) {
                missingStickers.remove(sticker);
            } else {
                this.collections.add(new StickerCollection(sticker, originUser));
            }
        }
    }

    private StickerCollection findCollection(Sticker sticker) {
        return this.collections.stream().filter(c -> c.isOf(sticker)).findAny().orElse(null);
    }

    public void removeSticker(Sticker sticker) throws FiguritaNoEncontradaException {
        StickerCollection collection = this.findCollection(sticker);
        if (collection != null) {
            collection.decreaseAmount();
        } else {
            throw new FiguritaNoEncontradaException("El usuario no posee la figurita " + sticker.getNumber());
        }
    }
}
