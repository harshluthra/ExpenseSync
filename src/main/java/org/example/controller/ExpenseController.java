package org.example.controller;

import org.example.dto.ExpenseRequest;
import org.example.dto.ExpenseResponse;
import org.example.dto.UserExpenseSummaryResponse;
import org.example.model.Expense;
import org.example.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(@RequestBody ExpenseRequest request) {

        ExpenseResponse expense = expenseService.createExpense(request);
        return ResponseEntity.ok(expense);
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @GetMapping("/user")
    public ResponseEntity<UserExpenseSummaryResponse> getExpensesByUser(@RequestParam String email,
                                                                        @RequestParam(defaultValue = "false") boolean showParticipants,
                                                                        Pageable pageable) {
        return ResponseEntity.ok(expenseService.getExpensesByUser(email, showParticipants, pageable));
    }
}
