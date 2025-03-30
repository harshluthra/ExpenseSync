package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserExpenseSummaryResponse {
    private BigDecimal netBalance; // Total balance (positive: receiving, negative: owes)
    private Page<ExpenseResponse> expenses; // Paginated list of expenses
}
