package org.example.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    @ManyToOne
    private User paidBy;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL)
    private List<ExpenseParticipant> participants = new ArrayList<>();
}
