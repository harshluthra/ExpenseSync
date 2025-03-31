package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateExpenseResponse {
    public Long id;
    public String description;
    public BigDecimal amount;
    public Map<String, String> paidBy;
    public List<ParticipantBreakdownDTO> participants;
    public LocalDateTime createdAt;
    public BigDecimal netTransactionBalance; // always null in this response
}
