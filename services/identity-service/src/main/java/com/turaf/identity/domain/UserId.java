package com.turaf.identity.domain;

import com.turaf.common.domain.ValueObject;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UserId extends ValueObject {
    
    private final String value;

    private UserId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or blank");
        }
        this.value = value;
    }

    public static UserId of(String value) {
        return new UserId(value);
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(value);
    }

    @Override
    public String toString() {
        return "UserId{" + value + '}';
    }
}
