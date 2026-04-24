package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import com.tacs.tp1c2026.exceptions.CuposAgotadosException;
import com.tacs.tp1c2026.exceptions.PropuestaNoCorrespondeException;
import com.tacs.tp1c2026.exceptions.PropuestaYaProcesadaException;
import com.tacs.tp1c2026.exceptions.UsuarioNoAutorizadoException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@NoArgsConstructor
@Document(collection = "publicaciones_intercambio")
@TypeAlias("publicacionIntercambio")
@AllArgsConstructor
@Setter
@Getter
public class PublicacionIntercambio {
  @Id
  private Integer id;

  @DocumentReference
  private Usuario publicante;

  @DocumentReference
  private FiguritaColeccion coleccion;

  private Integer cantidad;

  private LocalDateTime fechaCreacion = LocalDateTime.now();

  @DocumentReference
  private PropuestaIntercambio propuestaAceptada;

  private EstadoPublicacion estado = EstadoPublicacion.ACTIVA;

  private List<PropuestaIntercambio> propuestas = new ArrayList<>();

  private Feedback feedback;

  public PublicacionIntercambio(
          Usuario usuario,
          FiguritaColeccion coleccion,
          Integer cantidad
  ) {
    this.publicante = usuario;
    this.coleccion = coleccion;
    this.cantidad = cantidad;
  }


  /**
   * Verifica si hay cupos disponibles para nuevas propuestas.
   *
   * @return true si hay cupos disponibles
   */
  public boolean tieneCuposDisponibles() {
    long propuestasPendientes = this.propuestas.stream()
        .filter(p -> p.getEstado() == com.tacs.tp1c2026.entities.enums.EstadoPropuesta.PENDIENTE)
        .count();
    return propuestasPendientes < this.coleccion.getCantidadLibre();
  }

  /**
   * Valida si hay cupos disponibles para nuevas propuestas.
   *
   * @throws CuposAgotadosException si no hay cupos disponibles
   */
  public void validarCuposDisponibles() throws CuposAgotadosException {
    if (!tieneCuposDisponibles()) {
      throw new CuposAgotadosException("Ya no hay cupos para nuevas propuestas");
    }
  }

  /**
   * Valida que el usuario sea el dueño de esta publicación.
   *
   * @param usuario usuario a validar
   * @throws UsuarioNoAutorizadoException si el usuario no es el dueño
   */
  public void validarDueno(Usuario usuario) throws UsuarioNoAutorizadoException {
    Integer usuarioId = usuario.getId();
    Integer duenoId = this.publicante.getId();
    if (!Objects.equals(duenoId, usuarioId)) {
      throw new UsuarioNoAutorizadoException("El usuario no es el dueño de la publicacion");
    }
  }

  /**
   * Valida que una propuesta corresponda a esta publicación.
   *
   * @param propuesta propuesta a validar
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde a esta publicación
   */
  public void validarPropuestaCorresponde(PropuestaIntercambio propuesta) throws PropuestaNoCorrespondeException {
    if (!Objects.equals(propuesta.getPublicacion().getId(), this.id)) {
      throw new PropuestaNoCorrespondeException("La publicacion no corresponde a la propuesta");
    }
  }

  /**
   * Rechaza una propuesta de esta publicación.
   * Valida que la propuesta corresponda a esta publicación y que esté pendiente.
   *
   * @param propuesta propuesta a rechazar
   * @param usuarioSolicitante usuario que solicita el rechazo
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde
   * @throws UsuarioNoAutorizadoException si el usuario no es el dueño
   * @throws PropuestaYaProcesadaException si la propuesta ya fue procesada
   */
  public void rechazarPropuesta(PropuestaIntercambio propuesta, Usuario usuarioSolicitante)
      throws PropuestaNoCorrespondeException, UsuarioNoAutorizadoException, PropuestaYaProcesadaException {
    validarPropuestaCorresponde(propuesta);
    validarDueno(usuarioSolicitante);
    propuesta.validarPendiente();

    propuesta.rechazar();
    this.coleccion.reducirCantidadOfertada();
  }

  /**
   * Acepta una propuesta de esta publicación y ejecuta el intercambio.
   * Reduce el stock, transfiere figuritas y rechaza las demás propuestas.
   *
   * @param propuesta propuesta a aceptar
   * @param usuarioSolicitante usuario que solicita la aceptación
   * @throws PropuestaNoCorrespondeException si la propuesta no corresponde
   * @throws UsuarioNoAutorizadoException si el usuario no es el dueño
   * @throws PropuestaYaProcesadaException si la propuesta ya fue procesada
   */
  public void aceptarPropuesta(PropuestaIntercambio propuesta, Usuario usuarioSolicitante)
      throws PropuestaNoCorrespondeException, UsuarioNoAutorizadoException, PropuestaYaProcesadaException {
    validarPropuestaCorresponde(propuesta);
    validarDueno(usuarioSolicitante);
    propuesta.validarPendiente();

    propuesta.aceptar();
    this.propuestaAceptada = propuesta;
    this.coleccion.reducirCantidad();

    // Transferir figuritas al publicante
    propuesta.transferirFiguritasA(this.publicante);

    // Cerrar publicación si no hay más stock
    if (this.coleccion.getCantidadLibre() == 0) {
      this.estado = EstadoPublicacion.FINALIZADA;
    }

    // Rechazar las demás propuestas pendientes
    this.propuestas.stream()
        .filter(p -> !Objects.equals(p.getId(), propuesta.getId()))
        .filter(PropuestaIntercambio::estaPendiente)
        .forEach(PropuestaIntercambio::rechazar);
  }

  /**
   * Agrega una propuesta a esta publicación.
   *
   * @param propuesta propuesta a agregar
   */
  public void agregarPropuesta(PropuestaIntercambio propuesta) {
    this.propuestas.add(propuesta);
  }

  public void agregarFeedback(Feedback feedback){
    this.feedback = feedback;
  }

}
