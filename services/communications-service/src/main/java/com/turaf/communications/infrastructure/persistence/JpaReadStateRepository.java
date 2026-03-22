package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.ReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaReadStateRepository extends JpaRepository<ReadState, String> {
    
    Optional<ReadState> findByUserIdAndConversationId(String userId, String conversationId);
    
    List<ReadState> findByUserId(String userId);
}
