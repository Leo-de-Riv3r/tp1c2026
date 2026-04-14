package com.tacs.tp1c2026.repositories;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Converter
public class IntArrayConverter implements AttributeConverter<int[], String> {

    @Override
    public String convertToDatabaseColumn(int[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return null;
        }
        return Arrays.stream(attribute)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(","));
    }

    @Override
    public int[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new int[0];
        }
        String[] tokens = dbData.split(",");
        return Arrays.stream(tokens)
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}
