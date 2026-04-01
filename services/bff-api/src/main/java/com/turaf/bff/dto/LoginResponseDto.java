package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    
    private UserDto user;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType;
}
