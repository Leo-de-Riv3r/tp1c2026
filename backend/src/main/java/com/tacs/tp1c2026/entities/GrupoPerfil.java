// package com.tacs.tp1c2026.entities;
//
// import com.tacs.tp1c2026.properties.PerfilProperties;
//
// import java.util.*;
//
//
// import jakarta.annotation.PostConstruct;
// import lombok.AllArgsConstructor;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import org.springframework.data.annotation.Id;
// import org.springframework.data.mongodb.core.mapping.DBRef;
// import org.springframework.data.mongodb.core.mapping.Document;
//
// @Document("grupo_perfil")
// @NoArgsConstructor
// @Getter
// public class GrupoPerfil {
//
//     @Id
//     private Integer id;
//
//     private Perfil vectorRepresentativo = initializeVectorProfile();
//
//     @DBRef
//     private final List<Usuario> vecinos = new ArrayList<>();
//
//     private PerfilProperties properties;
//
//     public GrupoPerfil(PerfilProperties properties){
//         this.properties = properties;
//
//     }
//
//     /**
//     * Agrega un usuario como vecino del perfil.
//     * Si el perfil no tenía vecinos ni vector representativo, inicializa el vector
//      * con el perfil del primer vecino agregado.
//      *
//      * @param usuario usuario a agregar como vecino
//      */
//     public void agregarVecino(Usuario usuario) {
//         this.vecinos.add(usuario);
//     }
//
//     /**
//     * Elimina al usuario de la lista de vecinos del perfil.
//      *
//      * @param usuario usuario a remover
//      */
//     public void removerVecino(Usuario usuario) {
//         this.vecinos.add(usuario);
//     }
//
//     /**
//     * Actualiza el vector representativo del perfil calculando el promedio con signo
//      * de los perfiles vectoriales recibidos.
//      * Si la lista es nula o vacía, el vector se resetea a uno vacío.
//      *
//      * @param profiles lista de perfiles vectoriales a promediar
//      */
//     public void updateVector(List<Perfil> profiles) {
//         if (profiles == null || profiles.isEmpty()) {
//             this.vectorRepresentativo = Perfil.empty();
//             return;
//         }
//         this.vectorRepresentativo = Perfil.averageSign(profiles);
//     }
//
//     @PostConstruct
//     private Perfil initializeVectorProfile() {
//         assert properties != null;
//         int MAX_CARDS = properties.getMaxInitialCards();
//         Map<Integer,Integer> initialValues = new LinkedHashMap<>();
//
//         for (int i = 0; i < MAX_CARDS; i++) {
//             int randValue = (int) Math.round(Math.random() * 2 - 1);
//             initialValues.put(i, randValue);
//         }
//
//         return new Perfil(initialValues);
//     }
//
// }
