/*package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import javax.net.ssl.SSLSession;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
@Table
@Setter
@Getter
public class PublicacionIntercambio {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer id;
  @ManyToOne
  @JoinColumn(name = "usuario_id", referencedColumnName = "id")
  private Usuario publicante;
  @ManyToOne
  @JoinColumn(name = "figurita_id", referencedColumnName = "id")
  private FiguritaColeccion figuritaColeccion;
  @Column
  private LocalDateTime fechaCreacion = LocalDateTime.now();
  @OneToOne
  @JoinColumn(name = "propuesta_id", referencedColumnName = "id")
  private PropuestaIntercambio propuestaAceptada;
  @Enumerated(EnumType.STRING)
  @Column
  private EstadoPublicacion estado = EstadoPublicacion.ACTIVA;
}
*/