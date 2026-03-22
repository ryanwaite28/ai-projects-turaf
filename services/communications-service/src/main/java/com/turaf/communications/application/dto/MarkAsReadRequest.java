package com.turaf.communications.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsReadRequest {
    @NotNull(message = "Last message ID is required")
    private String lastMessageId;
}
