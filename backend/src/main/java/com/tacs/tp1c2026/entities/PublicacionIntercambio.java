package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.enums.EstadoPublicacion;
import java.time.LocalDateTime;

public class PublicacionIntercambio {
  private Usuario publicante;
  private Publicacion publicacion;
  private LocalDateTime fechaCreacion;
  private EstadoPublicacion estado;
}
