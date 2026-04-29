 package com.tacs.tp1c2026.entities.profiles;

 import com.tacs.tp1c2026.entities.card.Card;

 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;

 public class Profile {

     private final Map<Integer, Integer> values;

     public Profile(){
         this.values = new LinkedHashMap<>();
     }
     
     public Profile(Map<Integer, Integer> values) {
         this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
     }

     public Profile(List<Card> repeatedCards, List<Card> missingCards) {
         Map<Integer, Integer> values = new LinkedHashMap<>();
         for (Card missing : missingCards) {
              values.put(missing.getId(), 1);
         }
         for (Card repeated : repeatedCards) {
             values.put(repeated.getId(), -1);
         }
         this.values = values;
     }

     /**
      * Crea un {@code VectorProfile} vacío sin ninguna entrada.
      *
      * @return perfil vectorial vacío
      */
     public static Profile empty() {
         return new Profile(new LinkedHashMap<>());
     }

     public boolean isEmpty() {
         return values.isEmpty();
     }

     public void addMissingCard(Card card) {
          values.put(card.getId(), 1);
     }

     public void addRepeatedCard(Card card) {
          values.put(card.getId(), -1);
     }

      /**
       * Removes any entry related to the provided sticker collection from the profile.
       */
      public void removeCard(Card repeatedCard) {
          values.remove(repeatedCard.getId());
      }


     /**
      * Calcula el puntaje de complementariedad entre dos perfiles.
      * Por cada clave presente en ambos perfiles, suma 1 si los signos son opuestos
      * ({@code -1} vs {@code 1} o {@code 1} vs {@code -1}).
      *
      * @param first  primer perfil vectorial
      * @param second segundo perfil vectorial
      * @return puntaje de complementariedad (entero &ge; 0)
      */
     public static int complement(Profile first, Profile second) {
         if (first == null || second == null) {
             return 0;
         }

         int score = 0;
          for (Map.Entry<Integer, Integer> entry : first.values.entrySet()) {
             Integer currentValue = entry.getValue();
             if (currentValue == null || currentValue == 0) {
                 continue;
             }

             Integer otherValue = second.values.get(entry.getKey());
             if (otherValue == null || otherValue == 0) {
                 continue;
             }

             if ((currentValue == -1 && otherValue == 1) || (currentValue == 1 && otherValue == -1)) {
                 score++;
             }
         }
         return score;
     }

     /**
      * Calcula el puntaje de acuerdo entre dos perfiles.
      * Por cada clave donde el primer perfil tiene valor {@code 1} y el segundo también tiene {@code 1}, suma 1.
      *
      * @param first  primer perfil vectorial
      * @param second segundo perfil vectorial
      * @return puntaje de acuerdo (entero &ge; 0)
      */
     public static int agreement(Profile first, Profile second) {
         if (first == null || second == null) {
             return 0;
         }
         int score = 0;
         for (Map.Entry<Integer, Integer> entry : first.values.entrySet()) {
             Integer currentValue = entry.getValue();
             if (currentValue == null || currentValue != 1) {
                 continue;
             }
             Integer otherValue = second.values.get(entry.getKey());
             if (otherValue != null && otherValue == 1) {
                 score++;
             }
         }
         return score;
     }

     /**
      * Calcula el promedio con signo de una lista de perfiles vectoriales.
      * Para cada clave presente en algún perfil, promedia los valores y redondea al entero más cercano.
      *
      * @param profiles lista de perfiles a promediar; si es nula o vacía retorna un perfil vacío
      * @return perfil vectorial resultante del promedio
      */
     public static Profile averageSign(List<Profile> profiles) {
         if (profiles == null || profiles.isEmpty()) {
             return Profile.empty();
         }

         int profileCount = profiles.size();
          Map<Integer, Integer> sumMap = new LinkedHashMap<>();

         for (Profile profile : profiles) {
             if (profile == null) {
                 continue;
             }
              for (Map.Entry<Integer, Integer> entry : profile.values.entrySet()) {
                 Integer value = entry.getValue();
                 if (value != null) {
                     sumMap.merge(entry.getKey(), value, Integer::sum);
                 }
             }
         }

          Map<Integer, Integer> averageSignMap = new LinkedHashMap<>();
          for (Map.Entry<Integer, Integer> entry : sumMap.entrySet()) {
             long value = Math.round(entry.getValue() / (double) profileCount);
             averageSignMap.put(entry.getKey(), (int) value);
         }

         return new Profile(averageSignMap);
     }




 }
