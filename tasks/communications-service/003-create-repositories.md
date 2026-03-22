# Task: Create JPA Repositories

**Service**: Communications Service  
**Type**: Infrastructure Layer  
**Priority**: High  
**Estimated Time**: 2 hours  
**Dependencies**: 002-create-domain-model, 010-create-database-migrations

---

## Objective

Implement JPA repository interfaces and implementations for all domain entities, providing data access layer with custom queries.

---

## Acceptance Criteria

- [x] JPA repositories implemented for all entities
- [x] Custom queries implemented for complex operations
- [x] Repository tests pass
- [x] Follows repository pattern from domain interfaces

---

## Implementation

**File**: `infrastructure/persistence/JpaConversationRepository.java`

```java
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
    
    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 " +
           "WHERE c.type = 'DIRECT' AND p1.userId = :user1Id AND p2.userId = :user2Id")
    Optional<Conversation> findDirectConversation(@Param("user1Id") String user1Id, 
                                                   @Param("user2Id") String user2Id);
}
```

**File**: `infrastructure/persistence/JpaMessageRepository.java`

```java
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
```

**File**: `infrastructure/persistence/JpaReadStateRepository.java`

```java
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
```

**File**: `infrastructure/persistence/ConversationRepositoryImpl.java`

```java
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
```

---

## References

- **Spec**: `specs/communications-service.md` (Infrastructure Layer)
- **Domain**: `domain/repository/`
