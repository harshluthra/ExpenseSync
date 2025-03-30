package org.example.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
public class ExpenseRequest {
    private String description;
    private BigDecimal amount;
    private String paidByUserEmail;
    private Set<String> participantEmails;
}
