package org.example.controller;

import org.example.dto.CreateExpenseRequest;
import org.example.dto.CreateExpenseResponse;
import org.example.dto.UserExpenseSummary;
import org.example.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<CreateExpenseResponse> addExpense(@RequestBody CreateExpenseRequest request) {

        CreateExpenseResponse expense = expenseService.createExpense(request);
        return ResponseEntity.ok(expense);
    }

    @GetMapping
    public ResponseEntity<UserExpenseSummary> getExpensesByUserEmail(@RequestParam String email,
                                                                     @RequestParam(required = false, defaultValue = "false") boolean showParticipants) {
        return ResponseEntity.ok(expenseService.getExpensesByUserEmail(email, showParticipants));
    }
}
