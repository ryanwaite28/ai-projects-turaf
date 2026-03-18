package com.turaf.identity.domain;

import com.turaf.common.domain.ValueObject;

import java.util.List;
import java.util.regex.Pattern;

public class Password extends ValueObject {
    
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    
    private final String hashedValue;
    private transient final PasswordEncoder encoder;

    private Password(String hashedValue, PasswordEncoder encoder) {
        this.hashedValue = hashedValue;
        this.encoder = encoder;
    }

    public static Password fromRaw(String rawPassword, PasswordEncoder encoder) {
        validatePasswordStrength(rawPassword);
        String hashed = encoder.encode(rawPassword);
        return new Password(hashed, encoder);
    }

    public static Password fromHashed(String hashedValue, PasswordEncoder encoder) {
        if (hashedValue == null || hashedValue.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be null or blank");
        }
        return new Password(hashedValue, encoder);
    }

    public boolean matches(String rawPassword) {
        if (encoder == null) {
            throw new IllegalStateException("PasswordEncoder not available for verification");
        }
        return encoder.matches(rawPassword, hashedValue);
    }

    private static void validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters");
        }
        
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        
        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }

    public String getHashedValue() {
        return hashedValue;
    }

    @Override
    protected List<Object> getEqualityComponents() {
        return List.of(hashedValue);
    }

    @Override
    public String toString() {
        return "Password{***}";
    }
}
