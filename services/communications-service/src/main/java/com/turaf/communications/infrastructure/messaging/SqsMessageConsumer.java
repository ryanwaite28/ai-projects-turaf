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
