package org.example.service;

import org.example.dto.*;
import org.example.exception.ExpenseSyncException;
import org.example.model.Expense;
import org.example.model.ExpenseParticipant;
import org.example.model.User;
import org.example.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExpenseService handles creation and retrieval of shared group expenses,
 * including support for various split types (EQUAL, EXACT).
 */
@Service
public class ExpenseService {

    private final UserService userService;
    private final ExpenseRepository expenseRepository;

    public ExpenseService(UserService userService, ExpenseRepository expenseRepository) {
        this.userService = userService;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Creates a new expense and calculates share per participant.
     * Supports split types: EQUAL, EXACT.
     */
    public CreateExpenseResponse createExpense(CreateExpenseRequest request) {
        validatePaidByIncluded(request);

        Set<String> participantEmails = extractParticipantEmails(request);
        Set<User> participants = validateAndFetchParticipants(participantEmails);
        User payer = userService.getUserByEmail(request.getPaidByEmail());

        Expense expense = buildExpense(request, payer);
        Map<String, BigDecimal> emailToShareMap = calculateShares(request, participants);
        List<ParticipantBreakdownDTO> breakdownList = attachParticipants(expense, participants, emailToShareMap, payer, request);

        expenseRepository.save(expense);

        return buildCreateExpenseResponse(expense, payer, breakdownList);
    }

    /**
     * Retrieves a summary of expenses for a user, optionally showing participants.
     */
    public UserExpenseSummary getExpensesByUserEmail(String email, boolean showParticipants) {
        userService.getUserByEmail(email);

        List<Expense> expenses = expenseRepository.findAllByParticipantsUserEmail(email);
        BigDecimal netBalance = BigDecimal.ZERO;
        List<CreateExpenseResponse> summary = new ArrayList<>();

        for (Expense expense : expenses) {
            BigDecimal share = getUserShareFromExpense(expense, email);
            BigDecimal paid = expense.getPaidBy().getEmail().equals(email) ? expense.getAmount() : BigDecimal.ZERO;
            BigDecimal net = paid.subtract(share);
            netBalance = netBalance.add(net);

            CreateExpenseResponse response = buildExpenseSummaryResponse(expense, net, showParticipants);
            summary.add(response);
        }

        UserExpenseSummary result = new UserExpenseSummary();
        result.setNetBalance(netBalance);
        result.setExpenses(summary);
        return result;
    }

    /**
     * Returns all expenses for a specific user.
     */
    public List<Expense> getExpensesByUserEmail(String email) {
        return expenseRepository.findAllByParticipantsUserEmail(email);
    }

    /**
     * Returns all expenses in the system.
     */
    public List<Expense> fetchAllExpenses() {
        return expenseRepository.findAll();
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────

    private void validatePaidByIncluded(CreateExpenseRequest request) {
        if (request.getParticipants().stream().noneMatch(p -> p.getEmail().equals(request.getPaidByEmail()))) {
            throw new ExpenseSyncException("Paid by email must be a participant in the expense.");
        }
    }

    private Set<String> extractParticipantEmails(CreateExpenseRequest request) {
        return request.getParticipants().stream()
                .map(ParticipantDTO::getEmail)
                .collect(Collectors.toSet());
    }

    private Set<User> validateAndFetchParticipants(Set<String> emails) {
        Set<User> users = userService.getAllUsersByEmail(emails);
        if (users.size() != emails.size()) {
            throw new ExpenseSyncException("All emails must be valid.");
        }
        return users;
    }

    private Expense buildExpense(CreateExpenseRequest request, User payer) {
        Expense expense = new Expense();
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(payer);
        expense.setCreatedAt(LocalDateTime.now());
        return expense;
    }

    /**
     * Calculates how much each participant owes based on the selected split type.
     *
     * <p>
     * Supported split types:
     * <ul>
     *   <li><b>EQUAL</b>: The amount is divided equally among all participants.</li>
     *   <li><b>EXACT</b>: Each participant provides their specific share amount.
     *       Total of all shares must equal the full expense amount.</li>
     *   <li><b>PERCENT</b>: Not yet supported — throws exception if used.</li>
     * </ul>
     * </p>
     *
     * @param request      The original create expense request containing participants, amount, and split type.
     * @param participants Set of valid user entities participating in the expense.
     * @return A mapping of user email to their share of the expense.
     * @throws ExpenseSyncException if the EXACT shares do not sum up to the total amount or if PERCENT is used.
     */

    private Map<String, BigDecimal> calculateShares(CreateExpenseRequest request, Set<User> participants) {
        Map<String, BigDecimal> map = new HashMap<>();

        switch (request.getSplitType()) {
            case EQUAL -> {
                BigDecimal equalShare = request.getAmount().divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);
                participants.forEach(u -> map.put(u.getEmail(), equalShare));
            }
            case EXACT -> {
                BigDecimal total = request.getParticipants().stream()
                        .map(p -> Optional.ofNullable(p.getShare()).orElse(BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (total.compareTo(request.getAmount()) != 0) {
                    throw new ExpenseSyncException("Sum of shares must equal total amount for EXACT split.");
                }

                request.getParticipants().forEach(p ->
                        map.put(p.getEmail(), Optional.ofNullable(p.getShare()).orElse(BigDecimal.ZERO)));
            }
            case PERCENT -> throw new ExpenseSyncException("PERCENT split is not yet supported.");
        }
        return map;
    }

    private List<ParticipantBreakdownDTO> attachParticipants(
            Expense expense,
            Set<User> users,
            Map<String, BigDecimal> shareMap,
            User payer,
            CreateExpenseRequest request
    ) {
        List<ParticipantBreakdownDTO> breakdown = new ArrayList<>();

        for (User u : users) {
            BigDecimal share = shareMap.getOrDefault(u.getEmail(), BigDecimal.ZERO);

            ExpenseParticipant ep = new ExpenseParticipant();
            ep.setUser(u);
            ep.setExpense(expense);
            ep.setShareAmount(share);
            expense.getParticipants().add(ep);

            BigDecimal owed = u.getEmail().equals(payer.getEmail()) ? BigDecimal.ZERO : share;
            BigDecimal receive = u.getEmail().equals(payer.getEmail()) ? request.getAmount().subtract(share) : BigDecimal.ZERO;

            breakdown.add(new ParticipantBreakdownDTO(u.getName(), u.getEmail(), owed, receive));
        }

        return breakdown;
    }

    private CreateExpenseResponse buildCreateExpenseResponse(Expense expense, User payer, List<ParticipantBreakdownDTO> breakdown) {
        CreateExpenseResponse res = new CreateExpenseResponse();
        res.id = expense.getId();
        res.description = expense.getDescription();
        res.amount = expense.getAmount();
        res.paidBy = Map.of("name", payer.getName(), "email", payer.getEmail());
        res.participants = breakdown;
        res.createdAt = expense.getCreatedAt();
        res.netTransactionBalance = null;
        return res;
    }

    private BigDecimal getUserShareFromExpense(Expense expense, String email) {
        return expense.getParticipants().stream()
                .filter(p -> p.getUser().getEmail().equals(email))
                .findFirst()
                .map(ExpenseParticipant::getShareAmount)
                .orElse(BigDecimal.ZERO);
    }

    private CreateExpenseResponse buildExpenseSummaryResponse(Expense expense, BigDecimal net, boolean showParticipants) {
        CreateExpenseResponse response = new CreateExpenseResponse();
        response.setId(expense.getId());
        response.setDescription(expense.getDescription());
        response.setAmount(expense.getAmount());
        response.setCreatedAt(expense.getCreatedAt());
        response.setPaidBy(Map.of(
                "name", expense.getPaidBy().getName(),
                "email", expense.getPaidBy().getEmail()
        ));
        response.setNetTransactionBalance(net);

        if (showParticipants) {
            List<ParticipantBreakdownDTO> breakdown = expense.getParticipants().stream()
                    .map(p -> buildParticipantBreakdown(p, expense))
                    .toList();
            response.setParticipants(breakdown);
        } else {
            response.setParticipants(null);
        }

        return response;
    }

    private ParticipantBreakdownDTO buildParticipantBreakdown(ExpenseParticipant participant, Expense expense) {
        String email = participant.getUser().getEmail();
        boolean isPayer = email.equals(expense.getPaidBy().getEmail());
        BigDecimal owed = isPayer ? BigDecimal.ZERO : participant.getShareAmount();
        BigDecimal receive = isPayer ? expense.getAmount().subtract(participant.getShareAmount()) : BigDecimal.ZERO;

        return new ParticipantBreakdownDTO(
                participant.getUser().getName(),
                email,
                owed,
                receive
        );
    }
}
