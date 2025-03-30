package org.example.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_user", nullable = false)
    private User fromUser; // Who owes money

    @ManyToOne
    @JoinColumn(name = "to_user", nullable = false)
    private User toUser; // Who should receive the money

    @Column(nullable = false)
    private BigDecimal amount; // Amount owed

    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expense expense; // The expense that caused this transaction

    @CreationTimestamp
    private LocalDateTime createdAt;

}
