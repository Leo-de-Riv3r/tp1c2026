package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.exceptions.BadInputException;
import jakarta.annotation.PostConstruct;
import org.springframework.data.annotation.Id;
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
import com.tacs.tp1c2026.entities.embedded.FiguritaColeccion;
import com.tacs.tp1c2026.entities.embedded.FiguritaFaltante;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeAlias("usuario")
@Document(collection = "usuarios") // nombre de la collection en mongo
public class Usuario {
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
  private List<FiguritaColeccion> collection = new ArrayList<>();
  @Builder.Default
  private List<FiguritaFaltante> missingCards = new ArrayList<>();
  @Builder.Default
  private List<String> suggestionsIds = new ArrayList<>();

  public void agregarSugerencia(Usuario sugerencias) {
    this.sugerenciasIntercambios.add(sugerencias);
  }

  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.repetidas.add(figuritaColeccion);
    this.vectorProfile.addCard(figuritaColeccion);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.faltantes.add(figurita);
    this.vectorProfile.addCard(figurita);
  }

  public FiguritaColeccion getRepetidaByNumero(Integer numFiguritaPublicada) {
    FiguritaColeccion figuritaColeccion =  this.repetidas.stream()
        .filter(repetida -> repetida.getFigurita().getNumero().equals(numFiguritaPublicada))
        .findFirst()
        .orElse(null);
    if (figuritaColeccion == null) {
      throw new BadInputException("El usuario no posee la figurita " + numFiguritaPublicada);
    }
    return figuritaColeccion;
  }

  public void agregarAlerta(Alerta alerta) {
    this.alertas.add(alerta);
  }


  @PostConstruct
  private void initializeVectorProfile() {
    this.vectorProfile = new VectorProfile(this.repetidas, this.faltantes);
  }

  public void removerSugerencias() {
    this.suggestionsIds.clear();
  }
}
