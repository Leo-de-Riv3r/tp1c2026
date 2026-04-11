package com.tacs.tp1c2026.repository;

import com.tacs.tp1c2026.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<Usuario,Long> {
}
