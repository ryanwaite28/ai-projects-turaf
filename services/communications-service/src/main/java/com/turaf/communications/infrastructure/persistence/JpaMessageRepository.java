package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaMessageRepository extends JpaRepository<Message, String> {
    
    Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);
    
    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversationId = :conversationId " +
           "AND m.createdAt > COALESCE(" +
           "  (SELECT msg.createdAt FROM Message msg " +
           "   JOIN ReadState rs ON rs.lastReadMessageId = msg.id " +
           "   WHERE rs.userId = :userId AND rs.conversationId = :conversationId), " +
           "  TIMESTAMP '1970-01-01 00:00:00')")
    int countUnreadMessages(@Param("userId") String userId, 
                           @Param("conversationId") String conversationId);
}
