package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.exceptions.FiguritaNoDisponibleException;
import com.tacs.tp1c2026.exceptions.FiguritaNoEncontradaException;
import com.tacs.tp1c2026.exceptions.FiguritaYaPublicadaException;
import com.tacs.tp1c2026.exceptions.FiguritasInsuficientesException;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TypeAlias("usuario")
@Document(collection = "usuarios")
public class Usuario {
  @Id
  private Integer id;
  private String name;
  @Indexed(unique = true)
  private String email;
  private String passwordHash;
  private Integer avatarId;
  @Builder.Default
  private Double rating = null;
  @Builder.Default
  private Integer exchangesCount = 0;
  private LocalDateTime lastLogin;
  @Builder.Default
  private LocalDateTime creationDate = LocalDateTime.now();
  @Builder.Default
  private List<FiguritaColeccion> collection = new ArrayList<>();
  @Builder.Default
  private List<FiguritaFaltante> missingCards = new ArrayList<>();
  @Builder.Default
  private List<Integer> suggestionsIds = new ArrayList<>();
  @Builder.Default
  private List<Alerta> alertas = new ArrayList<>();

  @Builder.Default
  private Perfil perfil = new Perfil();
  public void agregarSugerencia(Usuario sugerencias) {
    this.suggestionsIds.add(sugerencias.id);
  }

  public void agregarRepetidas(FiguritaColeccion figuritaColeccion) {
    this.collection.add(figuritaColeccion);
    this.perfil.addCard(figuritaColeccion);
  }

  public void agregarFaltantes(Figurita figurita) {
    this.missingCards.add(new FiguritaFaltante(figurita));
    this.perfil.addCard(figurita);
  }

  public FiguritaColeccion getRepetidaByNumero(Integer numFiguritaPublicada) throws FiguritaNoEncontradaException {
    return this.collection.stream()
        .filter(repetida -> repetida.getFigurita().getNumber().equals(numFiguritaPublicada))
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
      figurita.aumentarCantidadOfertada();
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
    return figurita.crearPublicacion(this,numFigurita);
  }

  public void agregarAlerta(Alerta alerta) {
    this.alertas.add(alerta);
  }

  @PostConstruct
  private void initializeVectorProfile() {
    this.perfil = new Perfil(this.collection,this.missingCards);
  }

  public void removerSugerencias() {
    this.suggestionsIds.clear();
  }

}
