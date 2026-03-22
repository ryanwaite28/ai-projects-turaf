package com.turaf.communications.application.dto;

import com.turaf.communications.domain.model.ConversationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {
    
    @NotNull(message = "Conversation type is required")
    private ConversationType type;
    
    private String name;
    
    @NotNull(message = "Participant IDs are required")
    @Size(min = 1, message = "At least one participant is required")
    private List<String> participantIds;
}
