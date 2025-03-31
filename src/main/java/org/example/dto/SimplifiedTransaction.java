package org.example.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimplifiedTransaction {

    private Map<String, String> from;
    private Map<String, String> to;
    private BigDecimal amount;
}
