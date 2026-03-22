package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaConversationRepository extends JpaRepository<Conversation, String> {
    
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.userId = :userId")
    List<Conversation> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT DISTINCT c FROM Conversation c JOIN c.participants p1 " +
           "WHERE c.type = 'DIRECT' AND EXISTS " +
           "(SELECT 1 FROM Participant p2 WHERE p2.conversation = c AND p2.userId = :user2Id) " +
           "AND p1.userId = :user1Id")
    Optional<Conversation> findDirectConversation(@Param("user1Id") String user1Id, 
                                                   @Param("user2Id") String user2Id);
}
