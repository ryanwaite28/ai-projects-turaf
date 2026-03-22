package com.turaf.communications.interfaces.mapper;

import com.turaf.communications.application.dto.ConversationDTO;
import com.turaf.communications.application.dto.ParticipantDTO;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.Participant;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
    
    ConversationDTO toDTO(Conversation conversation);
    
    List<ConversationDTO> toDTOList(List<Conversation> conversations);
    
    ParticipantDTO toDTO(Participant participant);
    
    List<ParticipantDTO> toParticipantDTOList(List<Participant> participants);
}
