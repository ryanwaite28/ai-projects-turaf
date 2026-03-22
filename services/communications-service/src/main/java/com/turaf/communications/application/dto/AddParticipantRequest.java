package com.turaf.communications.application.dto;

import com.turaf.communications.domain.model.ParticipantRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddParticipantRequest {
    
    @NotNull(message = "User ID is required")
    private String userId;
    
    private ParticipantRole role = ParticipantRole.MEMBER;
}
