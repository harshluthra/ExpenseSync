package org.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CreateExpenseRequest {
    private String description;
    private BigDecimal amount;
    private String paidByEmail;
    private SplitType splitType;
    private List<ParticipantDTO> participants;
}
