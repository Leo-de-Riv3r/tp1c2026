// package com.tacs.tp1c2026.entities;

// import com.tacs.tp1c2026.entities.Figurita;
// import com.tacs.tp1c2026.entities.FiguritaColeccion;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// public class VectorProfile {

//     private final Map<Integer, Integer> values;

//     public VectorProfile(Map<Integer, Integer> values) {
//         this.values = values == null ? new HashMap<>() : new HashMap<>(values);
//     }

//     public VectorProfile(List<FiguritaColeccion> repetidas, List<Figurita> faltantes) {
//         Map<Integer, Integer> values = new HashMap<>();
//         if (faltantes != null) {
//             for (Figurita figurita : faltantes) {
//                 if (figurita != null && figurita.getId() != null) {
//                     values.put(figurita.getId(), 1);
//                 }
//             }
//         }
//         if (repetidas != null) {
//             for (FiguritaColeccion figuritaColeccion : repetidas) {
//                 if (figuritaColeccion != null && figuritaColeccion.getFigurita() != null && figuritaColeccion.getFigurita().getId() != null) {
//                     values.put(figuritaColeccion.getFigurita().getId(), -1);
//                 }
//             }
//         }
//         this.values = values;
//     }

//     /**
//      * Crea un {@code VectorProfile} vacío sin ninguna entrada.
//      *
//      * @return perfil vectorial vacío
//      */
//     public static VectorProfile empty() {
//         return new VectorProfile(new HashMap<>());
//     }

//     public boolean isEmpty() {
//         return values.isEmpty();
//     }

//     /**
//      * Serializa el perfil vectorial a una cadena de texto con el formato
//      * {@code clave:valor;clave:valor}, omitiendo las entradas con valor cero.
//      *
//      * @return cadena serializada o {@code null} si el vector está vacío o todos sus valores son cero
//      */
//     public String serialize() {
//         if (values.isEmpty()) {
//             return null;
//         }

//         StringBuilder builder = new StringBuilder();
//         values.entrySet().stream()
//             .filter(entry -> entry.getValue() != null && entry.getValue() != 0)
//             .sorted(Map.Entry.comparingByKey())
//             .forEach(entry -> {
//                 if (builder.length() > 0) {
//                     builder.append(';');
//                 }
//                 builder.append(entry.getKey()).append(':').append(entry.getValue());
//             });

//         return builder.length() == 0 ? null : builder.toString();
//     }

//     /**
//      * Reconstruye un {@code VectorProfile} a partir de su representación serializada.
//      * Las entradas que no puedan parsearse o cuyo valor sea cero son ignoradas.
//      *
//      * @param dbData cadena con formato {@code clave:valor;clave:valor}
//      * @return perfil vectorial reconstruido; perfil vacío si la cadena es nula o vacía
//      */
//     public static VectorProfile deserialize(String dbData) {
//         if (dbData == null || dbData.isBlank()) {
//             return VectorProfile.empty();
//         }

//         Map<Integer, Integer> result = new HashMap<>();
//         String[] entries = dbData.split(";");
//         for (String entry : entries) {
//             if (entry == null || entry.isBlank()) {
//                 continue;
//             }

//             String[] parts = entry.split(":", 2);
//             if (parts.length != 2) {
//                 continue;
//             }

//             try {
//                 Integer key = Integer.valueOf(parts[0]);
//                 Integer value = Integer.valueOf(parts[1]);
//                 if (value != 0) {
//                     result.put(key, value);
//                 }
//             } catch (NumberFormatException ignored) {
//             }
//         }

//         return new VectorProfile(result);
//     }

//     public static VectorProfile merge(VectorProfile base, VectorProfile update) {
//         Map<Integer, Integer> merged = new HashMap<>();

//         if (base != null) {
//             merged.putAll(base.values);
//         }

//         if (update != null) {
//             for (Map.Entry<Integer, Integer> entry : update.values.entrySet()) {
//                 Integer key = entry.getKey();
//                 Integer value = entry.getValue();
//                 if (key != null && value != null && value != 0) {
//                     merged.put(key, value);
//                 }
//             }
//         }

//         return new VectorProfile(merged);
//     }

//     public void addCard(Figurita figurita) {
//         if (figurita == null || figurita.getId() == null) {
//             return;
//         }
//         values.put(figurita.getId(), 1);
//     }

//     public void addCard(FiguritaColeccion figuritaColeccion) {
//         if (figuritaColeccion == null || figuritaColeccion.getFigurita() == null || figuritaColeccion.getFigurita().getId() == null) {
//             return;
//         }
//         values.put(figuritaColeccion.getFigurita().getId(), -1);
//     }

//     /**
//      * Calcula el puntaje de complementariedad entre dos perfiles.
//      * Por cada clave presente en ambos perfiles, suma 1 si los signos son opuestos
//      * ({@code -1} vs {@code 1} o {@code 1} vs {@code -1}).
//      *
//      * @param first  primer perfil vectorial
//      * @param second segundo perfil vectorial
//      * @return puntaje de complementariedad (entero &ge; 0)
//      */
//     public static int complement(VectorProfile first, VectorProfile second) {
//         if (first == null || second == null) {
//             return 0;
//         }

//         int score = 0;
//         for (Map.Entry<Integer, Integer> entry : first.values.entrySet()) {
//             Integer currentValue = entry.getValue();
//             if (currentValue == null || currentValue == 0) {
//                 continue;
//             }

//             Integer otherValue = second.values.get(entry.getKey());
//             if (otherValue == null || otherValue == 0) {
//                 continue;
//             }

//             if ((currentValue == -1 && otherValue == 1) || (currentValue == 1 && otherValue == -1)) {
//                 score++;
//             }
//         }
//         return score;
//     }

//     /**
//      * Calcula el promedio con signo de una lista de perfiles vectoriales.
//      * Para cada clave presente en algún perfil, promedia los valores y redondea al entero más cercano.
//      *
//      * @param profiles lista de perfiles a promediar; si es nula o vacía retorna un perfil vacío
//      * @return perfil vectorial resultante del promedio
//      */
//     public static VectorProfile averageSign(List<VectorProfile> profiles) {
//         if (profiles == null || profiles.isEmpty()) {
//             return VectorProfile.empty();
//         }

//         int profileCount = profiles.size();
//         Map<Integer, Integer> sumMap = new HashMap<>();

//         for (VectorProfile profile : profiles) {
//             if (profile == null) {
//                 continue;
//             }
//             for (Map.Entry<Integer, Integer> entry : profile.values.entrySet()) {
//                 Integer value = entry.getValue();
//                 if (value != null) {
//                     sumMap.merge(entry.getKey(), value, Integer::sum);
//                 }
//             }
//         }

//         Map<Integer, Integer> averageSignMap = new HashMap<>();
//         for (Map.Entry<Integer, Integer> entry : sumMap.entrySet()) {
//             long value = Math.round(entry.getValue() / (double) profileCount);
//             averageSignMap.put(entry.getKey(), (int) value);
//         }

//         return new VectorProfile(averageSignMap);
//     }

//     /**
//      * Calcula el puntaje de acuerdo entre dos perfiles.
//      * Por cada clave donde el primer perfil tiene valor {@code 1} y el segundo también tiene {@code 1}, suma 1.
//      *
//      * @param first  primer perfil vectorial
//      * @param second segundo perfil vectorial
//      * @return puntaje de acuerdo (entero &ge; 0)
//      */
//     public static int agreement(VectorProfile first, VectorProfile second) {
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


// }
