 package com.tacs.tp1c2026.entities.profiles;

 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;

 public class Profile {

   private final Map<String, Integer> values;

   public Profile(){
     this.values = new LinkedHashMap<>();
   }

   public Profile(Map<String, Integer> values) {
     this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
   }

   public Profile(List<String> repeatedCardIds, List<String> missingCardIds) {
     Map<String, Integer> values = new LinkedHashMap<>();
     for (String missingId : missingCardIds) {
       values.put(missingId, 1);
     }
     for (String repeatedId : repeatedCardIds) {
       values.put(repeatedId, -1);
     }
     this.values = values;
   }

   public static Profile empty() {
     return new Profile(new LinkedHashMap<>());
   }

   public boolean isEmpty() {
     return values.isEmpty();
   }

   public void addMissingCard(String cardId) {
     if (values.getOrDefault(cardId, 0) != 1) {
       values.put(cardId, 1);
     }
   }

   public void addRepeatedCard(String cardId) {
     // Solo hace el put si la carta no está o si su valor es distinto de -1
     if (values.getOrDefault(cardId, 0) != -1) {
       values.put(cardId, -1);
     }
   }

   public void removeCard(String cardId) {
     values.remove(cardId);
   }


   public static int complement(Profile first, Profile second) {
     if (first == null || second == null) {
       return 0;
     }

     int score = 0;
     for (Map.Entry<String, Integer> entry : first.values.entrySet()) {
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

   public static int agreement(Profile first, Profile second) {
     if (first == null || second == null) {
       return 0;
     }
     int score = 0;
     for (Map.Entry<String, Integer> entry : first.values.entrySet()) {
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

   public static Profile averageSign(List<Profile> profiles) {
     if (profiles == null || profiles.isEmpty()) {
       return Profile.empty();
     }

     int profileCount = profiles.size();
     Map<String, Integer> sumMap = new LinkedHashMap<>();

     for (Profile profile : profiles) {
       if (profile == null) {
         continue;
       }
       for (Map.Entry<String, Integer> entry : profile.values.entrySet()) {
         Integer value = entry.getValue();
         if (value != null) {
           sumMap.merge(entry.getKey(), value, Integer::sum);
         }
       }
     }

     Map<String, Integer> averageSignMap = new LinkedHashMap<>();
     for (Map.Entry<String, Integer> entry : sumMap.entrySet()) {
       long value = Math.round(entry.getValue() / (double) profileCount);
       averageSignMap.put(entry.getKey(), (int) value);
     }

     return new Profile(averageSignMap);
   }
 }
