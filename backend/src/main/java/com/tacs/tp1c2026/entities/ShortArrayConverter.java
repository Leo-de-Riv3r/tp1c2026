package com.tacs.tp1c2026.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.util.Base64;

@Converter
public class ShortArrayConverter implements AttributeConverter<short[], String> {

    @Override
    public String convertToDatabaseColumn(short[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return null;
        }

        int length = attribute.length;
        int byteCount = (length * 2 + 7) / 8;
        byte[] packed = new byte[4 + byteCount];
        ByteBuffer.wrap(packed).putInt(length);

        int bitIndex = 0;
        for (short value : attribute) {
            int code;
            switch (value) {
                case 0 -> code = 0;
                case 1 -> code = 1;
                case -1 -> code = 2;
                default -> throw new IllegalArgumentException("Short array values must be -1, 0, or 1");
            }
            int bytePos = 4 + (bitIndex >> 3);
            int shift = 6 - (bitIndex & 7);
            packed[bytePos] |= (code << shift);
            bitIndex += 2;
        }

        return Base64.getEncoder().encodeToString(packed);
    }

    @Override
    public short[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new short[0];
        }

        byte[] packed = Base64.getDecoder().decode(dbData);
        if (packed.length < 4) {
            return new short[0];
        }

        ByteBuffer buffer = ByteBuffer.wrap(packed);
        int length = buffer.getInt();
        short[] result = new short[length];

        for (int i = 0; i < length; i++) {
            int bitIndex = i * 2;
            int bytePos = 4 + (bitIndex >> 3);
            int shift = 6 - (bitIndex & 7);
            int code = (packed[bytePos] >> shift) & 0b11;
            result[i] = switch (code) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> -1;
                default -> 0;
            };
        }

        return result;
    }
}
