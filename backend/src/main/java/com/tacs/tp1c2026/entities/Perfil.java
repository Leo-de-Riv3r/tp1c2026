// package com.tacs.tp1c2026.entities;

// import com.tacs.tp1c2026.properties.PerfilProperties;
// import com.tacs.tp1c2026.repositories.VectorProfileConverter;

// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;

// import jakarta.persistence.Column;
// import jakarta.persistence.Convert;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;
// import jakarta.persistence.JoinColumn;
// import jakarta.persistence.JoinTable;
// import jakarta.persistence.ManyToMany;
// import jakarta.persistence.Table;
// import jakarta.persistence.Transient;


// @Entity
// @Table(name = "perfiles")
// public class Perfil {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Integer id;

//     @Column(name = "vector_representativo")
//     @Convert(converter = VectorProfileConverter.class)
//     private VectorProfile vectorRepresentativo = initializeVectorProfile();

//     @ManyToMany
//     @JoinTable(
//         name = "perfil_vecinos",
//         joinColumns = @JoinColumn(name = "perfil_id"),
//         inverseJoinColumns = @JoinColumn(name = "usuario_id")
//     )
//     private Set<Usuario> vecinos = new HashSet<>();

//     @Transient
//     private PerfilProperties properties;

//     public VectorProfile getVectorProfile() {
//         return vectorRepresentativo;
//     }

//     /**
//     * Agrega un usuario como vecino del perfil.
//     * Si el perfil no tenía vecinos ni vector representativo, inicializa el vector
//      * con el perfil del primer vecino agregado.
//      *
//      * @param usuario usuario a agregar como vecino
//      */
//     public void agregarVecino(Usuario usuario) {
//         if (this.vecinos.isEmpty() && this.vectorRepresentativo.isEmpty() && usuario.getVectorProfile() != null) {
//             this.vectorRepresentativo = usuario.getVectorProfile();
//         }
//         this.vecinos.add(usuario);
//     }

//     /**
//     * Elimina al usuario de la lista de vecinos del perfil.
//      *
//      * @param usuario usuario a remover
//      */
//     public void removerVecino(Usuario usuario) {
//         this.vecinos.remove(usuario);
//     }

//     public Set<Usuario> getVecinos() {
//         return vecinos;
//     }

//     /**
//     * Actualiza el vector representativo del perfil calculando el promedio con signo
//      * de los perfiles vectoriales recibidos.
//      * Si la lista es nula o vacía, el vector se resetea a uno vacío.
//      *
//      * @param profiles lista de perfiles vectoriales a promediar
//      */
//     public void updateVector(List<VectorProfile> profiles) {
//         if (profiles == null || profiles.isEmpty()) {
//             this.vectorRepresentativo = VectorProfile.empty();
//             return;
//         }

//         this.vectorRepresentativo = VectorProfile.averageSign(profiles);
//     }

//     private VectorProfile initializeVectorProfile() {
//         int MAX_CARDS = properties.maxInitialCards();
//         Map<Integer,Integer> initialValues = new HashMap<>();

//         for (int i = 0; i < MAX_CARDS; i++) {
//             int randValue = (int) Math.round(Math.random() * 2 - 1);
//             initialValues.put(i, randValue);
//         }

//         VectorProfile vectorProfile = new VectorProfile(initialValues);
//         return vectorProfile;
//     }

// }
