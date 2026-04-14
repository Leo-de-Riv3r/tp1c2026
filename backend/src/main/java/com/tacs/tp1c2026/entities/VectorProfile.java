package com.tacs.tp1c2026.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VectorProfile {

    private final Map<Integer, Integer> values;

    public VectorProfile(Map<Integer, Integer> values) {
        this.values = values == null ? new HashMap<>() : new HashMap<>(values);
    }

    public static VectorProfile empty() {
        return new VectorProfile(new HashMap<>());
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public String serialize() {
        if (values.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        values.entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue() != 0)
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                if (builder.length() > 0) {
                    builder.append(';');
                }
                builder.append(entry.getKey()).append(':').append(entry.getValue());
            });

        return builder.length() == 0 ? null : builder.toString();
    }

    public static VectorProfile deserialize(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return VectorProfile.empty();
        }

        Map<Integer, Integer> result = new HashMap<>();
        String[] entries = dbData.split(";");
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            try {
                Integer key = Integer.valueOf(parts[0]);
                Integer value = Integer.valueOf(parts[1]);
                if (value != 0) {
                    result.put(key, value);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return new VectorProfile(result);
    }

    public static class Builder {
        private final Map<Integer, Integer> values = new HashMap<>();

        public Builder set(Integer key, Integer value) {
            if (key == null || value == null || value == 0) {
                return this;
            }
            values.put(key, value);
            return this;
        }

        public VectorProfile build() {
            return new VectorProfile(values);
        }
    }

    public int complement(VectorProfile other) {
        int score = 0;
        for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
            Integer currentValue = entry.getValue();
            if (currentValue == null || currentValue == 0) {
                continue;
            }

            Integer otherValue = other.values.get(entry.getKey());
            if (otherValue == null || otherValue == 0) {
                continue;
            }

            if ((currentValue == -1 && otherValue == 1) || (currentValue == 1 && otherValue == -1)) {
                score++;
            }
        }
        return score;
    }

    public static VectorProfile averageSign(List<VectorProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return VectorProfile.empty();
        }

        int profileCount = profiles.size();
        Map<Integer, Integer> sumMap = new HashMap<>();

        for (VectorProfile profile : profiles) {
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

        Map<Integer, Integer> averageSignMap = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : sumMap.entrySet()) {
            long value = Math.round(entry.getValue() / (double) profileCount);
            averageSignMap.put(entry.getKey(), (int) value);
        }

        return new VectorProfile(averageSignMap);
    }

    public int agreement(VectorProfile other) {
        int score = 0;
        for (Map.Entry<Integer, Integer> entry : values.entrySet()) {
            Integer currentValue = entry.getValue();
            if (currentValue == null || currentValue != 1) {
                continue;
            }

            Integer otherValue = other.values.get(entry.getKey());
            if (otherValue != null && otherValue == 1) {
                score++;
            }
        }
        return score;
    }


}
