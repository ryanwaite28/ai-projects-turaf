package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.Message;
import com.turaf.communications.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {
    
    private final JpaMessageRepository jpaRepository;
    
    @Override
    public Message save(Message message) {
        return jpaRepository.save(message);
    }
    
    @Override
    public Optional<Message> findById(String id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable) {
        return jpaRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
    }
    
    @Override
    public int countUnreadMessages(String userId, String conversationId) {
        return jpaRepository.countUnreadMessages(userId, conversationId);
    }
}
