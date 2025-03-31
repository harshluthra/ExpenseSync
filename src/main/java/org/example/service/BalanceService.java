package org.example.service;

import org.example.dto.RawBalanceResponse;
import org.example.dto.RawTransaction;
import org.example.dto.SimplifiedBalanceResponse;
import org.example.dto.SimplifiedTransaction;
import org.example.model.Expense;
import org.example.model.ExpenseParticipant;
import org.example.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BalanceService handles computation of raw and simplified balances for a user.
 * - Raw Balances: shows all individual debts without minimizing transactions
 * - Simplified Balances: calculates optimized minimal transactions required to settle debts
 */
@Service
public class BalanceService {

    private static final String EMAIL_KEY = "email";

    private final UserService userService;
    private final ExpenseService expenseService;

    public BalanceService(UserService userService, ExpenseService expenseService) {
        this.userService = userService;
        this.expenseService = expenseService;
    }

    public RawBalanceResponse getRawBalance(String email) {
        User user = userService.getUserByEmail(email);

        Map<String, String> currentUserMap = createUserMap(user);

        List<Expense> expenses = expenseService.getExpensesByUserEmail(email);

        Map<String, BigDecimal> balances = computeParticipantBalances(user, expenses);

        List<RawTransaction> transactions = buildRawTransactions(balances, currentUserMap);
        BigDecimal netBalance = transactions.stream()
                .map(txn -> txn.getFrom().equals(currentUserMap) ? txn.getAmount().negate() : txn.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RawBalanceResponse response = new RawBalanceResponse();
        response.setUser(currentUserMap);
        response.setNetBalance(netBalance);
        response.setTransactions(transactions);

        return response;
    }

    /**
     * Computes the simplified balances for a given user by reducing the number of transactions.
     */
    public SimplifiedBalanceResponse getSimplifiedBalance(String email) {
        List<Expense> expenses = expenseService.fetchAllExpenses();
        Map<String, BigDecimal> balanceMap = calculateNetBalances(expenses);

        // Capture netBalance before we zero out during simplification
        BigDecimal netBalance = balanceMap.getOrDefault(email, BigDecimal.ZERO);

        List<SimplifiedTransaction> simplified = minimizeTransactions(balanceMap);

        User currentUser = userService.getUserByEmail(email);

        Map<String, String> currentUserMap = createUserMap(currentUser);

        List<SimplifiedTransaction> userTransactions = filterTransactionsForUser(simplified, email);

        SimplifiedBalanceResponse response = new SimplifiedBalanceResponse();
        response.setUser(currentUserMap);
        response.setNetBalance(netBalance);
        response.setTransactions(userTransactions);

        return response;
    }

    /**
     * Computes net balance per user from all expenses.
     */
    private Map<String, BigDecimal> calculateNetBalances(List<Expense> expenses) {
        Map<String, BigDecimal> map = new HashMap<>();

        for (Expense expense : expenses) {
            BigDecimal share = expense.getAmount().divide(BigDecimal.valueOf(expense.getParticipants().size()), 2, RoundingMode.HALF_UP);

            for (ExpenseParticipant participant : expense.getParticipants()) {
                String email = participant.getUser().getEmail();
                map.merge(email, share.negate(), BigDecimal::add);
            }

            String payerEmail = expense.getPaidBy().getEmail();
            map.merge(payerEmail, expense.getAmount(), BigDecimal::add);
        }

        return map;
    }

    /**
     * Minimizes the number of transactions between debtors and creditors.
     */
    private List<SimplifiedTransaction> minimizeTransactions(Map<String, BigDecimal> balanceMap) {
        List<SimplifiedTransaction> result = new ArrayList<>();

        List<Map.Entry<String, BigDecimal>> creditors = balanceMap.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Map.Entry.comparingByValue())
                .toList();

        List<Map.Entry<String, BigDecimal>> debtors = balanceMap.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) < 0)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();

        int i = 0;
        int j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<String, BigDecimal> debtor = debtors.get(i);
            Map.Entry<String, BigDecimal> creditor = creditors.get(j);

            BigDecimal amount = debtor.getValue().abs().min(creditor.getValue());

            User fromUser = userService.getUserByEmail(debtor.getKey());
            User toUser = userService.getUserByEmail(creditor.getKey());

            if (fromUser != null && toUser != null) {
                result.add(new SimplifiedTransaction(createUserMap(fromUser), createUserMap(toUser), amount));
            }

            balanceMap.put(debtor.getKey(), debtor.getValue().add(amount));
            balanceMap.put(creditor.getKey(), creditor.getValue().subtract(amount));

            if (balanceMap.get(debtor.getKey()).compareTo(BigDecimal.ZERO) == 0) i++;
            if (balanceMap.get(creditor.getKey()).compareTo(BigDecimal.ZERO) == 0) j++;
        }

        return result;
    }

    /**
     * Filters a user's relevant simplified transactions from all transactions.
     */
    private List<SimplifiedTransaction> filterTransactionsForUser(List<SimplifiedTransaction> all, String email) {
        return all.stream()
                .filter(txn -> txn.getFrom().get(EMAIL_KEY).equals(email) || txn.getTo().get(EMAIL_KEY).equals(email))
                .toList();
    }

    /**
     * Utility method to convert User entity into name-email map.
     */
    private Map<String, String> createUserMap(User user) {
        return Map.of("name", user.getName(), EMAIL_KEY, user.getEmail());
    }

    private Map<String, BigDecimal> computeParticipantBalances(User user, List<Expense> expenses) {
        Map<String, BigDecimal> balances = new HashMap<>();

        for (Expense e : expenses) {
            BigDecimal total = e.getAmount();
            int size = e.getParticipants().size();
            BigDecimal share = total.divide(BigDecimal.valueOf(size), 2, RoundingMode.HALF_UP);
            boolean isPayer = e.getPaidBy().getEmail().equals(user.getEmail());

            for (ExpenseParticipant p : e.getParticipants()) {
                String participantEmail = p.getUser().getEmail();
                if (participantEmail.equals(user.getEmail())) continue;

                if (isPayer) {
                    balances.merge(participantEmail, share, BigDecimal::add);
                } else if (participantEmail.equals(e.getPaidBy().getEmail())) {
                    balances.merge(participantEmail, share.negate(), BigDecimal::add);
                }
            }
        }
        return balances;
    }

    private List<RawTransaction> buildRawTransactions(Map<String, BigDecimal> balances, Map<String, String> currentUserMap) {
        List<RawTransaction> transactions = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            BigDecimal amt = entry.getValue();
            if (amt.compareTo(BigDecimal.ZERO) == 0) continue;

            User counterparty = userService.getUserByEmail(entry.getKey());

            Map<String, String> counterpartyMap = createUserMap(counterparty);

            if (amt.compareTo(BigDecimal.ZERO) > 0) {
                transactions.add(new RawTransaction(counterpartyMap, currentUserMap, amt));
            } else {
                transactions.add(new RawTransaction(currentUserMap, counterpartyMap, amt.abs()));
            }
        }
        return transactions;
    }
}
