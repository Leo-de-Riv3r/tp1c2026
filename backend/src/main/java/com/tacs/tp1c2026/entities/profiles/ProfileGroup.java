 package com.tacs.tp1c2026.entities.profiles;

 import com.tacs.tp1c2026.entities.user.User;
 import lombok.Getter;
 import org.springframework.data.annotation.Id;
 import org.springframework.data.annotation.TypeAlias;
 import org.springframework.data.mongodb.core.mapping.Document;
 import org.springframework.data.mongodb.core.mapping.DocumentReference;

 import java.util.HashSet;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;

 @Document("grupo_perfil")
 @TypeAlias("profile_group")
 public class ProfileGroup {

     @Id
     private String id;

    @Getter
    private Profile representativeProfile = new Profile();

    @DocumentReference
    @Getter
    private Set<User> neighbours = new HashSet<>();

     /**
     * Agrega un usuario como vecino del perfil.
     * Si el perfil no tenía vecinos ni vector representativo, inicializa el vector
      * con el perfil del primer vecino agregado.
      *
      * @param neighborUser usuario a agregar como vecino
      */
     public void addNeighbor(User neighborUser) {
         this.neighbours.add(neighborUser);
     }

     /**
     * Elimina al usuario de la lista de vecinos del perfil.
      *
       * @param neighborUser usuario a remover
      */
     public void removeNeighbor(User neighborUser) {
          this.neighbours.remove(neighborUser);
     }

     /** Vacía la lista de vecinos; se reconstruye en cada corrida del cron de sugerencias. */
     public void clearNeighbours() {
          this.neighbours.clear();
     }

     /**
     * Actualiza el vector representativo del perfil calculando el promedio con signo
      * de los perfiles vectoriales recibidos.
      * Si la lista es nula o vacía, el vector se resetea a uno vacío.
      *
      */
     public void updateVector() {
         if (this.neighbours.isEmpty()) {
             return;
         }
         this.representativeProfile = Profile.averageSign(neighbours.stream().map(User::getProfile).toList());
     }

     /**
      * Inicializa el perfil representativo con un vector aleatorio sobre el catálogo dado
      * (cada figurita vale -1, 0 o 1). Se usa al sembrar los grupos: arranca disperso para que
      * los usuarios se distribuyan por similitud, y luego {@link #updateVector()} ajusta el
      * representativo al promedio de sus vecinos.
      *
      * @param catalogCardIds ids de las figuritas del catálogo (dimensiones del vector)
      */
     public void initializeRepresentative(List<String> catalogCardIds) {
          Map<String, Integer> initialValues = new LinkedHashMap<>();
          for (String cardId : catalogCardIds) {
              int randomValue = (int) Math.round(Math.random() * 2 - 1);
              initialValues.put(cardId, randomValue);
          }
          this.representativeProfile = new Profile(initialValues);
     }

 }
