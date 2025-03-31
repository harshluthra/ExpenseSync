package org.example.service;

import org.example.dto.CreateExpenseRequest;
import org.example.dto.CreateExpenseResponse;
import org.example.dto.ParticipantBreakdownDTO;
import org.example.dto.UserExpenseSummary;
import org.example.exception.ExpenseSyncException;
import org.example.model.Expense;
import org.example.model.ExpenseParticipant;
import org.example.model.User;
import org.example.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExpenseService {

    private final UserService userService;
    private final ExpenseRepository expenseRepository;

    public ExpenseService(UserService userService, ExpenseRepository expenseRepository) {
        this.userService = userService;
        this.expenseRepository = expenseRepository;
    }

    public CreateExpenseResponse createExpense(CreateExpenseRequest request) {
        if (!request.getParticipantEmails().contains(request.getPaidByEmail())) {
            throw new ExpenseSyncException("Paid by email must be a participant in the expense.");
        }

        Set<User> participants = userService.getAllUsersByEmail(request.getParticipantEmails());
        if (participants.size() != request.getParticipantEmails().size()) {
            throw new ExpenseSyncException("All emails must be valid.");
        }

        User payer = userService.getUserByEmail(request.getPaidByEmail());

        Expense expense = new Expense();
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(payer);
        expense.setCreatedAt(LocalDateTime.now());

        BigDecimal share = request.getAmount().divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);

        List<ParticipantBreakdownDTO> breakdownList = new ArrayList<>();

        for (User u : participants) {
            ExpenseParticipant ep = new ExpenseParticipant();
            ep.setUser(u);
            ep.setExpense(expense);
            ep.setShareAmount(share);
            expense.getParticipants().add(ep);

            BigDecimal owed = u.getEmail().equals(payer.getEmail()) ? BigDecimal.ZERO : share;
            BigDecimal receive = u.getEmail().equals(payer.getEmail()) ? request.getAmount().subtract(share) : BigDecimal.ZERO;

            breakdownList.add(new ParticipantBreakdownDTO(u.getName(), u.getEmail(), owed, receive));
        }

        expenseRepository.save(expense);

        CreateExpenseResponse response = new CreateExpenseResponse();
        response.id = expense.getId();
        response.description = expense.getDescription();
        response.amount = expense.getAmount();
        response.paidBy = Map.of("name", payer.getName(), "email", payer.getEmail());
        response.participants = breakdownList;
        response.createdAt = expense.getCreatedAt();
        response.netTransactionBalance = null;

        return response;
    }

    public UserExpenseSummary getExpensesByUserEmail(String email, boolean showParticipants) {
        userService.getUserByEmail(email);

        List<Expense> expenses = expenseRepository.findAllByParticipantsUserEmail(email);
        BigDecimal netBalance = BigDecimal.ZERO;
        List<CreateExpenseResponse> summary = new ArrayList<>();

        for (Expense expense : expenses) {
            BigDecimal share = expense.getParticipants().stream()
                    .filter(p -> p.getUser().getEmail().equals(email))
                    .findFirst().map(ExpenseParticipant::getShareAmount).orElse(BigDecimal.ZERO);

            BigDecimal paid = expense.getPaidBy().getEmail().equals(email) ? expense.getAmount() : BigDecimal.ZERO;
            BigDecimal net = paid.subtract(share);
            netBalance = netBalance.add(net);

            CreateExpenseResponse res = new CreateExpenseResponse();
            res.setId(expense.getId());
            res.setDescription(expense.getDescription());
            res.setAmount(expense.getAmount());
            res.setCreatedAt(expense.getCreatedAt());
            res.setPaidBy(Map.of("name", expense.getPaidBy().getName(), "email", expense.getPaidBy().getEmail()));
            res.setNetTransactionBalance(net);

            if (showParticipants) {
                List<ParticipantBreakdownDTO> breakdown = expense.getParticipants().stream().map(p -> {
                    BigDecimal owed = p.getUser().getEmail().equals(expense.getPaidBy().getEmail()) ? BigDecimal.ZERO : p.getShareAmount();
                    BigDecimal receive = p.getUser().getEmail().equals(expense.getPaidBy().getEmail()) ? expense.getAmount().subtract(p.getShareAmount()) : BigDecimal.ZERO;
                    return new ParticipantBreakdownDTO(p.getUser().getName(), p.getUser().getEmail(), owed, receive);
                }).toList();

                res.setParticipants(breakdown);
            } else {
                res.setParticipants(null);
            }

            summary.add(res);
        }

        UserExpenseSummary result = new UserExpenseSummary();
        result.setNetBalance(netBalance);
        result.setExpenses(summary);
        return result;
    }

    public List<Expense> getExpensesByUserEmail(String email) {
        return expenseRepository.findAllByParticipantsUserEmail(email);
    }

    public List<Expense> fetchAllExpenses() {
        return expenseRepository.findAll();
    }
}
