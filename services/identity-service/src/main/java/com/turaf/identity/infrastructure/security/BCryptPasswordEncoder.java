package com.turaf.identity.infrastructure.security;

import com.turaf.identity.domain.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordEncoder implements PasswordEncoder {

    private final org.springframework.security.crypto.password.PasswordEncoder springEncoder;

    public BCryptPasswordEncoder() {
        this.springEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
    }

    @Override
    public String encode(String rawPassword) {
        return springEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return springEncoder.matches(rawPassword, encodedPassword);
    }
}
