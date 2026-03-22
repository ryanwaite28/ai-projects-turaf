package com.turaf.communications.application.dto;

import com.turaf.communications.domain.model.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {
    private String id;
    private String userId;
    private ParticipantRole role;
    private Instant joinedAt;
}
