package com.tacs.tp1c2026.entities.bucket;

import com.tacs.tp1c2026.entities.VectorProfile;
import com.tacs.tp1c2026.repositories.VectorProfileConverter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tacs.tp1c2026.entities.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;


@Entity
@Table(name = "buckets")
public class Bucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "vector_representativo")
    @Convert(converter = VectorProfileConverter.class)
    private VectorProfile vectorRepresentativo = VectorProfile.empty();

    @ManyToMany
    @JoinTable(
        name = "bucket_vecinos",
        joinColumns = @JoinColumn(name = "bucket_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<Usuario> vecinos = new HashSet<>();

    /**
     * Calcula la afinidad del bucket con un perfil vectorial de usuario
     * usando la métrica de acuerdo sobre el vector representativo del bucket.
     *
     * @param vectorUsuario perfil vectorial del usuario
     * @return puntaje de afinidad (mayor indica mayor similitud)
     */
    public int calcularAfinidad(VectorProfile vectorUsuario) {
        return vectorRepresentativo.agreement(vectorUsuario);
    }

    public VectorProfile getVectorProfile() {
        return vectorRepresentativo;
    }

    /**
     * Agrega un usuario como vecino del bucket.
     * Si el bucket no tenía vecinos ni vector representativo, inicializa el vector
     * con el perfil del primer vecino agregado.
     *
     * @param usuario usuario a agregar como vecino
     */
    public void agregarVecino(Usuario usuario) {
        if (this.vecinos.isEmpty() && this.vectorRepresentativo.isEmpty() && usuario.getVectorProfile() != null) {
            this.vectorRepresentativo = usuario.getVectorProfile();
        }
        this.vecinos.add(usuario);
    }

    /**
     * Elimina al usuario de la lista de vecinos del bucket.
     *
     * @param usuario usuario a remover
     */
    public void removerVecino(Usuario usuario) {
        this.vecinos.remove(usuario);
    }

    public Set<Usuario> getVecinos() {
        return vecinos;
    }

    /**
     * Actualiza el vector representativo del bucket calculando el promedio con signo
     * de los perfiles vectoriales recibidos.
     * Si la lista es nula o vacía, el vector se resetea a uno vacío.
     *
     * @param profiles lista de perfiles vectoriales a promediar
     */
    public void updateVector(List<VectorProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            this.vectorRepresentativo = VectorProfile.empty();
            return;
        }

        this.vectorRepresentativo = VectorProfile.averageSign(profiles);
    }

}
