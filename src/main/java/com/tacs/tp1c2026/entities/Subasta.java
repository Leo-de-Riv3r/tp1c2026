package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.ReglasStrategies.IReglaStrategy;
import java.time.LocalDateTime;
import java.util.List;

public class Subasta {
  private Figurita figurita;
  private Usuario publicante;
  private LocalDateTime fechaCreacion;
  private LocalDateTime fechaCierre;
  private List<IReglaStrategy> condicionesMinimas;
  private OfertaSubasta mejorApuesta;
}
