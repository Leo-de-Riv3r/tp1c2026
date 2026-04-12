package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuariosRepository extends JpaRepository<Usuario, Integer> {
}
