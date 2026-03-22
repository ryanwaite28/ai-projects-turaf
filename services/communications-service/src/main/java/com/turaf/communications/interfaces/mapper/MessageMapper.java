package com.turaf.communications.interfaces.mapper;

import com.turaf.communications.application.dto.MessageDTO;
import com.turaf.communications.domain.model.Message;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageDTO toDTO(Message message);
    List<MessageDTO> toDTOList(List<Message> messages);
}
