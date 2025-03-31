package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimplifiedBalanceResponse {
    private Map<String, String> user;
    private BigDecimal netBalance;
    private List<SimplifiedTransaction> transactions;
}
