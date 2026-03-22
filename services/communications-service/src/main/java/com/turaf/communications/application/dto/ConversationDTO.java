package com.turaf.communications.application.dto;

import com.turaf.communications.domain.model.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private String id;
    private ConversationType type;
    private String name;
    private List<ParticipantDTO> participants;
    private Instant createdAt;
    private Instant updatedAt;
}
