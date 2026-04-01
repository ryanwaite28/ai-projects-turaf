package com.turaf.communications.interfaces.mapper;

import com.turaf.communications.application.dto.MessageDTO;
import com.turaf.communications.domain.model.Message;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-30T06:48:14-0400",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class MessageMapperImpl implements MessageMapper {

    @Override
    public MessageDTO toDTO(Message message) {
        if ( message == null ) {
            return null;
        }

        MessageDTO.MessageDTOBuilder messageDTO = MessageDTO.builder();

        messageDTO.content( message.getContent() );
        messageDTO.conversationId( message.getConversationId() );
        messageDTO.createdAt( message.getCreatedAt() );
        messageDTO.id( message.getId() );
        messageDTO.senderId( message.getSenderId() );

        return messageDTO.build();
    }

    @Override
    public List<MessageDTO> toDTOList(List<Message> messages) {
        if ( messages == null ) {
            return null;
        }

        List<MessageDTO> list = new ArrayList<MessageDTO>( messages.size() );
        for ( Message message : messages ) {
            list.add( toDTO( message ) );
        }

        return list;
    }
}
