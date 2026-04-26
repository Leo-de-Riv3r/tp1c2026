 package com.tacs.tp1c2026.entities;

 import com.tacs.tp1c2026.properties.ProfileProperties;

 import java.util.*;


 import jakarta.annotation.PostConstruct;
 import lombok.Getter;
 import org.springframework.data.annotation.Id;
 import org.springframework.data.mongodb.core.mapping.Document;
 import org.springframework.data.mongodb.core.mapping.DocumentReference;

 @Document("grupo_perfil")
 public class ProfileGroup {

     @Id
     private Integer id;

     @Getter
     private Profile representativeProfile = initializeVectorProfile();

     @DocumentReference
     @Getter
     private final Set<User> neighbours = new HashSet<>();

     private final ProfileProperties properties;

     public ProfileGroup(ProfileProperties properties){
         this.properties = properties;

     }

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

     @PostConstruct
     private Profile initializeVectorProfile() {
         assert properties != null;
          int maxCards = properties.getTotalNumberOfStickers();
          Map<Integer, Integer> initialValues = new LinkedHashMap<>();

          for (int index = 0; index < maxCards; index++) {
              int randomValue = (int) Math.round(Math.random() * 2 - 1);
              initialValues.put(index, randomValue);
         }

         return new Profile(initialValues);
     }

 }
