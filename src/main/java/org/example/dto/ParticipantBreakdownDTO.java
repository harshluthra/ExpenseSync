package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantBreakdownDTO {
    public String name;
    public String email;
    public BigDecimal amountOwed;
    public BigDecimal amountToReceive;
}
