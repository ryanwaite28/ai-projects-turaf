# Task: Implement SQS Consumers

**Service**: Communications Service  
**Type**: Infrastructure Layer - Messaging  
**Priority**: High  
**Estimated Time**: 3 hours  
**Dependencies**: 005-implement-message-service

---

## Objective

Implement SQS FIFO queue consumers to receive messages from the WebSocket Gateway and persist them to the database.

---

## Acceptance Criteria

- [x] SQS listeners configured for both queues
- [x] Message deserialization working
- [x] Messages persisted to database
- [x] Error handling and retry logic implemented
- [x] Dead letter queue handling configured

---

## Implementation

**File**: `infrastructure/messaging/SqsMessageConsumer.java`

```java
package com.turaf.communications.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.application.dto.MessageCreatedDTO;
import com.turaf.communications.application.service.MessageService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SqsMessageConsumer {
    
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    
    @SqsListener(value = "${aws.sqs.direct-messages-queue}")
    public void consumeDirectMessage(String messageJson) {
        try {
            log.info("Received direct message from SQS: {}", messageJson);
            MessageCreatedDTO dto = objectMapper.readValue(messageJson, MessageCreatedDTO.class);
            messageService.processMessage(dto);
            log.info("Successfully processed direct message: {}", dto.getId());
        } catch (Exception e) {
            log.error("Error processing direct message: {}", messageJson, e);
            throw new RuntimeException("Failed to process direct message", e);
        }
    }
    
    @SqsListener(value = "${aws.sqs.group-messages-queue}")
    public void consumeGroupMessage(String messageJson) {
        try {
            log.info("Received group message from SQS: {}", messageJson);
            MessageCreatedDTO dto = objectMapper.readValue(messageJson, MessageCreatedDTO.class);
            messageService.processMessage(dto);
            log.info("Successfully processed group message: {}", dto.getId());
        } catch (Exception e) {
            log.error("Error processing group message: {}", messageJson, e);
            throw new RuntimeException("Failed to process group message", e);
        }
    }
}
```

**File**: `infrastructure/config/SqsConfig.java`

```java
package com.turaf.communications.infrastructure.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import java.net.URI;

@Configuration
public class SqsConfig {
    
    @Value("${aws.region}")
    private String region;
    
    @Value("${aws.sqs.endpoint:}")
    private String endpoint;
    
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        var builder = SqsAsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());
        
        // LocalStack support
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
        return builder.build();
    }
    
    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
        SqsAsyncClient sqsAsyncClient
    ) {
        return SqsMessageListenerContainerFactory
            .builder()
            .sqsAsyncClient(sqsAsyncClient)
            .build();
    }
}
```

**File**: `application/dto/MessageCreatedDTO.java`

```java
package com.turaf.communications.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreatedDTO {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private String createdAt;
}
```

---

## Testing

**File**: `src/test/java/com/turaf/communications/infrastructure/messaging/SqsMessageConsumerTest.java`

```java
package com.turaf.communications.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turaf.communications.application.dto.MessageCreatedDTO;
import com.turaf.communications.application.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsMessageConsumerTest {
    
    @Mock
    private MessageService messageService;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private SqsMessageConsumer consumer;
    
    @Test
    void consumeDirectMessage_shouldProcessMessage() throws Exception {
        String json = "{\"id\":\"msg-1\",\"conversationId\":\"conv-1\",\"senderId\":\"user-1\",\"content\":\"Hello\"}";
        MessageCreatedDTO dto = new MessageCreatedDTO("msg-1", "conv-1", "user-1", "Hello", "2024-01-01T00:00:00Z");
        
        when(objectMapper.readValue(json, MessageCreatedDTO.class)).thenReturn(dto);
        
        consumer.consumeDirectMessage(json);
        
        verify(messageService).processMessage(dto);
    }
}
```

---

## Verification

1. Start LocalStack:
   ```bash
   docker-compose up -d localstack
   ```

2. Create SQS queues:
   ```bash
   aws --endpoint-url=http://localhost:4566 sqs create-queue \
     --queue-name communications-direct-messages.fifo \
     --attributes FifoQueue=true,ContentBasedDeduplication=true
   ```

3. Send test message:
   ```bash
   aws --endpoint-url=http://localhost:4566 sqs send-message \
     --queue-url http://localhost:4566/000000000000/communications-direct-messages.fifo \
     --message-body '{"id":"test-1","conversationId":"conv-1","senderId":"user-1","content":"Test"}' \
     --message-group-id conv-1
   ```

4. Verify message consumed and persisted

---

## References

- **Spec**: `specs/communications-service.md` (Infrastructure - Messaging)
- **AWS Docs**: Spring Cloud AWS SQS
