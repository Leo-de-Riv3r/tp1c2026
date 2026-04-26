package com.tacs.tp1c2026.entities.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.tacs.tp1c2026.entities.user.embedded.CollectionCard;
import com.tacs.tp1c2026.entities.user.embedded.MissingCard;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeAlias("user")
@Document(collection = "users") // nombre de la collection en mongo
public class User {
  @Id
  private String id;
  private String name;
  @Indexed(unique = true)
  private String email;
  private String passwordHash;
  private String avatarId; // vamos a hacer 5 svgs fijos para el front nada mas
  @Builder.Default
  private Double rating = null;
  @Builder.Default
  private Integer exchangesCount = 0;
  private LocalDateTime lastLogin;
  @Builder.Default
  private LocalDateTime creationDate = LocalDateTime.now();
  // Figuritas de la colección del usuario, cambio la lista de repetidas por esta porque serían las de esta colección con cantidad > 1
  @Builder.Default
  private List<CollectionCard> collection = new ArrayList<>();
  @Builder.Default
  private List<MissingCard> missingCards = new ArrayList<>();
  @Builder.Default
  private List<String> suggestionsIds = new ArrayList<>();

  // @Transient // no se guarda en mongo
  // private VectorProfile vectorProfile;

  // desde el botón "agregar figurita" del perfil
  public void addToCollection(CollectionCard newCard) {
    collection.stream()
      .filter(f -> f.getCardId().equals(newCard.getCardId()))
      .findFirst()
      .ifPresentOrElse(
        f -> f.setQuantity(f.getQuantity() + newCard.getQuantity()),
        () -> collection.add(newCard)  
      );
  }

  // Desde el botón "agregar faltantes" del perfil
  public void addToMissingCards(MissingCard missingCard) {
    boolean alreadyExists = this.missingCards.stream()
        .anyMatch(f -> f.getCardId().equals(missingCard.getCardId()));
    if (!alreadyExists) {
      this.missingCards.add(missingCard);
    }
  }

  // para saber cuantas figuritas puede realmente ofrecer
  // sería total - comprometidas (ya ofrecidas o propuestas)
  public int getAvailableQuantity(String cardId) {
    return collection.stream()
      .filter(f -> f.getCardId().equals(cardId))
      .mapToInt(f -> f.getQuantity() - f.getCompromisedCount())
      .findFirst()
      .orElse(0); 
    }
  
  // public VectorProfile getVectorProfile() {;
  //   if (this.vectorProfile == null) {
  //     this.vectorProfile = new VectorProfile(this.collection, this.missingCards);
  //   }
  //   return this.vectorProfile;
  // }

  public void removerSugerencias() {
    this.suggestionsIds.clear();
  }
}
