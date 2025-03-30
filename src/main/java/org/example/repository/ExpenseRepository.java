package org.example.repository;

import org.example.model.Expense;
import org.example.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // Fetch all expenses where the user is the payer OR a participant
    Page<Expense> findByPaidByOrParticipantsContaining(User paidBy, User participant, Pageable pageable);
}
