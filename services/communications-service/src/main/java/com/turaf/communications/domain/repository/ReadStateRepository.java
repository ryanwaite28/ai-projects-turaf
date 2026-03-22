package com.turaf.communications.domain.repository;

import com.turaf.communications.domain.model.ReadState;
import java.util.List;
import java.util.Optional;

public interface ReadStateRepository {
    ReadState save(ReadState readState);
    Optional<ReadState> findByUserIdAndConversationId(String userId, String conversationId);
    List<ReadState> findByUserId(String userId);
}
