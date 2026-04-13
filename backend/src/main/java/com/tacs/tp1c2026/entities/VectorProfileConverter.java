package com.tacs.tp1c2026.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class VectorProfileConverter implements AttributeConverter<VectorProfile, String> {

    @Override
    public String convertToDatabaseColumn(VectorProfile attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.serialize();
    }

    @Override
    public VectorProfile convertToEntityAttribute(String dbData) {
        return VectorProfile.deserialize(dbData);
    }
}
