package com.turaf.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String organizationId;
    private String role;
    private LocalDateTime joinedAt;
}
