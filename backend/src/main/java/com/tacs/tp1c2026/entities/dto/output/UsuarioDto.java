package com.tacs.tp1c2026.entities.dto.output;

import com.tacs.tp1c2026.entities.Figurita;
import com.tacs.tp1c2026.entities.FiguritaColeccion;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UsuarioDto {
  private Integer id;
  private String nombre;
  private LocalDateTime fechaAlta;
}
