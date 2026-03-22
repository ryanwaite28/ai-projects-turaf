package com.turaf.communications.infrastructure.persistence;

import com.turaf.communications.domain.model.ReadState;
import com.turaf.communications.domain.repository.ReadStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReadStateRepositoryImpl implements ReadStateRepository {
    
    private final JpaReadStateRepository jpaRepository;
    
    @Override
    public ReadState save(ReadState readState) {
        return jpaRepository.save(readState);
    }
    
    @Override
    public Optional<ReadState> findByUserIdAndConversationId(String userId, String conversationId) {
        return jpaRepository.findByUserIdAndConversationId(userId, conversationId);
    }
    
    @Override
    public List<ReadState> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId);
    }
}
