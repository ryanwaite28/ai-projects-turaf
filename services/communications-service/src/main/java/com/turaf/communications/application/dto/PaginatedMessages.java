package com.turaf.communications.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedMessages {
    private List<MessageDTO> messages;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
