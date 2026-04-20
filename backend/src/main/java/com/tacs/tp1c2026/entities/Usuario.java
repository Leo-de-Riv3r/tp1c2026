package com.tacs.tp1c2026.entities;

import jakarta.annotation.PostConstruct;
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
    @Transient
    private VectorProfile vectorProfile;
  public void agregarSugerencia(Usuario sugerencias) {
    this.suggestionsIds.add(sugerencias.id);
  }

  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.collection.add(figuritaColeccion);
    this.vectorProfile.addCard(figuritaColeccion);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.faltantes.add(figurita);
    this.vectorProfile.addCard(figurita);
  }

  public FiguritaColeccion getRepetidaByNumero(Integer numFiguritaPublicada) throws FiguritaNoEncontradaException {
    return this.repetidas.stream()
        .filter(repetida -> repetida.getFigurita().getNumero().equals(numFiguritaPublicada))
        .findFirst()
        .orElseThrow(() -> new FiguritaNoEncontradaException("El usuario no posee la figurita " + numFiguritaPublicada));
  }

  /**
   * Obtiene las figuritas repetidas indicadas por sus números y valida que estén disponibles para oferta.
   *
   * @param numerosFiguritas lista de números de figuritas a obtener
   * @return lista de FiguritaColeccion encontradas y disponibles para oferta
   * @throws FiguritaNoEncontradaException si alguna figurita no se encuentra
   * @throws FiguritaNoDisponibleException si alguna figurita no está disponible para oferta
   */
  public List<FiguritaColeccion> obtenerFiguritasParaOferta(List<Integer> numerosFiguritas)
      throws FiguritaNoEncontradaException, FiguritaNoDisponibleException {
    List<FiguritaColeccion> figuritasEncontradas = new ArrayList<>();

    for (Integer numFigurita : numerosFiguritas) {
      FiguritaColeccion figurita = getRepetidaByNumero(numFigurita);
      figurita.validarDisponibleParaOferta();
      figuritasEncontradas.add(figurita);
    }

    return figuritasEncontradas;
  }

  /**
   * Crea una publicación de intercambio para una figurita repetida.
   *
   * @param numFigurita número de la figurita a publicar
   * @return la publicación creada
   * @throws FiguritaNoEncontradaException si la figurita no se encuentra
   * @throws com.tacs.tp1c2026.exceptions.FiguritaYaPublicadaException si ya está publicada
   * @throws com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException si no hay stock
   */
  public PublicacionIntercambio publicarFigurita(Integer numFigurita)
      throws FiguritaNoEncontradaException,
          FiguritaYaPublicadaException,
          FiguritasInsuficientesException {
    FiguritaColeccion figurita = getRepetidaByNumero(numFigurita);
    return figurita.crearPublicacion(1);
  }

  public void agregarAlerta(Alerta alerta) {
    this.alertas.add(alerta);
  }

  public void removerSugerencias() {
    this.sugerenciasIntercambios.clear();
  }

  @PostConstruct
  private void initializeVectorProfile() {
    this.vectorProfile = new VectorProfile(this.repetidas, this.faltantes);
  }

  public void removerSugerencias() {
    this.suggestionsIds.clear();
  }

}
