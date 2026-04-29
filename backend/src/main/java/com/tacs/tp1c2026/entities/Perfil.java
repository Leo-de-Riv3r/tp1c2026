// package com.tacs.tp1c2026.entities;
//
// import java.util.LinkedHashMap;
// import java.util.List;
// import java.util.Map;
//
// public class Perfil {
//
//     private final Map<Integer, Integer> values;
//
//     public Perfil(){
//         this.values = new LinkedHashMap<>();
//     }
//
//     public Perfil(Map<Integer, Integer> values) {
//         this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
//     }
//
//     public Perfil(List<CardCollection> repetidas, List<FiguritaFaltante> faltantes) {
//         Map<Integer, Integer> values = new LinkedHashMap<>();
//         if (faltantes != null) {
//             for (FiguritaFaltante card : faltantes) {
//                 if (card != null && card.getCard().getId() != null) {
//                     values.put(card.getCard().getId(), 1);
//                 }
//             }
//         }
//         if (repetidas != null) {
//             for (CardCollection figuritaColeccion : repetidas) {
//                 if (figuritaColeccion != null && figuritaColeccion.getCard() != null && figuritaColeccion.getCard().getId() != null) {
//                     values.put(figuritaColeccion.getCard().getId(), -1);
//                 }
//             }
//         }
//         this.values = values;
//     }
//
//     /**
//      * Crea un {@code VectorProfile} vacío sin ninguna entrada.
//      *
//      * @return perfil vectorial vacío
//      */
//     public static Perfil empty() {
//         return new Perfil(new LinkedHashMap<>());
//     }
//
//     public boolean isEmpty() {
//         return values.isEmpty();
//     }
//
//     public void addCard(Card card) {
//         if (card == null || card.getId() == null) {
//             return;
//         }
//         values.put(card.getId(), 1);
//     }
//
//     public void addCard(CardCollection figuritaColeccion) {
//         if (figuritaColeccion == null || figuritaColeccion.getCard() == null || figuritaColeccion.getCard().getId() == null) {
//             return;
//         }
//         values.put(figuritaColeccion.getCard().getId(), -1);
//     }
//
//     /**
//      * Calcula el puntaje de complementariedad entre dos perfiles.
//      * Por cada clave presente en ambos perfiles, suma 1 si los signos son opuestos
//      * ({@code -1} vs {@code 1} o {@code 1} vs {@code -1}).
//      *
//      * @param first  primer perfil vectorial
//      * @param second segundo perfil vectorial
//      * @return puntaje de complementariedad (entero &ge; 0)
//      */
//     public static int complement(Perfil first, Perfil second) {
//         if (first == null || second == null) {
//             return 0;
//         }
//
//         int score = 0;
//          for (Map.Entry<Integer, Integer> entry : first.values.entrySet()) {
//             Integer currentValue = entry.getValue();
//             if (currentValue == null || currentValue == 0) {
//                 continue;
//             }
//
//             Integer otherValue = second.values.get(entry.getKey());
//             if (otherValue == null || otherValue == 0) {
//                 continue;
//             }
//
//             if ((currentValue == -1 && otherValue == 1) || (currentValue == 1 && otherValue == -1)) {
//                 score++;
//             }
//         }
//         return score;
//     }
//
//     /**
//      * Calcula el puntaje de acuerdo entre dos perfiles.
//      * Por cada clave donde el primer perfil tiene valor {@code 1} y el segundo también tiene {@code 1}, suma 1.
//      *
//      * @param first  primer perfil vectorial
//      * @param second segundo perfil vectorial
//      * @return puntaje de acuerdo (entero &ge; 0)
//      */
//     public static int agreement(Perfil first, Perfil second) {
//         if (first == null || second == null) {
//             return 0;
//         }
//         int score = 0;
//         for (Map.Entry<Integer, Integer> entry : first.values.entrySet()) {
//             Integer currentValue = entry.getValue();
//             if (currentValue == null || currentValue != 1) {
//                 continue;
//             }
//             Integer otherValue = second.values.get(entry.getKey());
//             if (otherValue != null && otherValue == 1) {
//                 score++;
//             }
//         }
//         return score;
//     }
//
//     /**
//      * Calcula el promedio con signo de una lista de perfiles vectoriales.
//      * Para cada clave presente en algún perfil, promedia los valores y redondea al entero más cercano.
//      *
//      * @param profiles lista de perfiles a promediar; si es nula o vacía retorna un perfil vacío
//      * @return perfil vectorial resultante del promedio
//      */
//     public static Perfil averageSign(List<Perfil> profiles) {
//         if (profiles == null || profiles.isEmpty()) {
//             return Perfil.empty();
//         }
//
//         int profileCount = profiles.size();
//          Map<Integer, Integer> sumMap = new LinkedHashMap<>();
//
//         for (Perfil profile : profiles) {
//             if (profile == null) {
//                 continue;
//             }
//              for (Map.Entry<Integer, Integer> entry : profile.values.entrySet()) {
//                 Integer value = entry.getValue();
//                 if (value != null) {
//                     sumMap.merge(entry.getKey(), value, Integer::sum);
//                 }
//             }
//         }
//
//          Map<Integer, Integer> averageSignMap = new LinkedHashMap<>();
//          for (Map.Entry<Integer, Integer> entry : sumMap.entrySet()) {
//             long value = Math.round(entry.getValue() / (double) profileCount);
//             averageSignMap.put(entry.getKey(), (int) value);
//         }
//
//         return new Perfil(averageSignMap);
//     }
//
//
// }
//
//