package com.turaf.communications.interfaces.mapper;

import com.turaf.communications.application.dto.ConversationDTO;
import com.turaf.communications.application.dto.ParticipantDTO;
import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.model.Participant;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-22T13:39:34-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Amazon.com Inc.)"
)
@Component
public class ConversationMapperImpl implements ConversationMapper {

    @Override
    public ConversationDTO toDTO(Conversation conversation) {
        if ( conversation == null ) {
            return null;
        }

        ConversationDTO.ConversationDTOBuilder conversationDTO = ConversationDTO.builder();

        conversationDTO.id( conversation.getId() );
        conversationDTO.type( conversation.getType() );
        conversationDTO.name( conversation.getName() );
        conversationDTO.participants( toParticipantDTOList( conversation.getParticipants() ) );
        conversationDTO.createdAt( conversation.getCreatedAt() );
        conversationDTO.updatedAt( conversation.getUpdatedAt() );

        return conversationDTO.build();
    }

    @Override
    public List<ConversationDTO> toDTOList(List<Conversation> conversations) {
        if ( conversations == null ) {
            return null;
        }

        List<ConversationDTO> list = new ArrayList<ConversationDTO>( conversations.size() );
        for ( Conversation conversation : conversations ) {
            list.add( toDTO( conversation ) );
        }

        return list;
    }

    @Override
    public ParticipantDTO toDTO(Participant participant) {
        if ( participant == null ) {
            return null;
        }

        ParticipantDTO.ParticipantDTOBuilder participantDTO = ParticipantDTO.builder();

        participantDTO.id( participant.getId() );
        participantDTO.userId( participant.getUserId() );
        participantDTO.role( participant.getRole() );
        participantDTO.joinedAt( participant.getJoinedAt() );

        return participantDTO.build();
    }

    @Override
    public List<ParticipantDTO> toParticipantDTOList(List<Participant> participants) {
        if ( participants == null ) {
            return null;
        }

        List<ParticipantDTO> list = new ArrayList<ParticipantDTO>( participants.size() );
        for ( Participant participant : participants ) {
            list.add( toDTO( participant ) );
        }

        return list;
    }
}
