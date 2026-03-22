package com.turaf.communications.domain.repository;

import com.turaf.communications.domain.model.Conversation;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(String id);
    List<Conversation> findByUserId(String userId);
    Optional<Conversation> findDirectConversation(String user1Id, String user2Id);
    void delete(String id);
}
