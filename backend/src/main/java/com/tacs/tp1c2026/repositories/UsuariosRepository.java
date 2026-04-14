package com.tacs.tp1c2026.repositories;

import com.tacs.tp1c2026.entities.Usuario;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuariosRepository extends JpaRepository<Usuario, Integer> {

    @Query("select u from Usuario u")
    Stream<Usuario> streamAll();
}
