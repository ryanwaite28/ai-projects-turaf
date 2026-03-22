package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.Conversation;
import com.turaf.communications.domain.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepository {
    
    private final JpaConversationRepository jpaRepository;
    
    @Override
    public Conversation save(Conversation conversation) {
        return jpaRepository.save(conversation);
    }
    
    @Override
    public Optional<Conversation> findById(String id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public List<Conversation> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId);
    }
    
    @Override
    public Optional<Conversation> findDirectConversation(String user1Id, String user2Id) {
        return jpaRepository.findDirectConversation(user1Id, user2Id);
    }
    
    @Override
    public void delete(String id) {
        jpaRepository.deleteById(id);
    }
}
