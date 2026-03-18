package com.turaf.identity.application.dto;

import com.turaf.identity.domain.User;

import java.time.Instant;

public class UserDto {

    private String id;
    private String email;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;

    public UserDto() {
    }

    public UserDto(String id, String email, String name, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserDto fromDomain(User user) {
        return new UserDto(
            user.getId().getValue(),
            user.getEmail().getValue(),
            user.getName(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
