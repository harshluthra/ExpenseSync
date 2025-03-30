package org.example.service;

import org.example.dto.*;
import org.example.exception.UserNotFoundException;
import org.example.model.Expense;
import org.example.model.Transaction;
import org.example.model.User;
import org.example.repository.ExpenseRepository;
import org.example.repository.TransactionRepository;
import org.example.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final UserService userService;
    private final ExpenseRepository expenseRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public ExpenseService(UserService userService, ExpenseRepository expenseRepository, TransactionRepository transactionRepository, UserRepository userRepository) {
        this.userService = userService;
        this.expenseRepository = expenseRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private static BigDecimal getNetBalance(Expense expense, User user) {
        BigDecimal totalAmount = expense.getAmount();
        int numberOfParticipants = expense.getParticipants().size();
        BigDecimal individualShare = totalAmount.divide(BigDecimal.valueOf(numberOfParticipants), 2, RoundingMode.HALF_UP);

        BigDecimal netBalance;

        if (user.equals(expense.getPaidBy())) {
            // If the user paid, they should receive money from others
            netBalance = totalAmount.subtract(individualShare);
        } else {
            // If the user is a participant, they owe their share
            netBalance = individualShare.negate();
        }
        return netBalance;
    }

    @Transactional
    public ExpenseResponse createExpense(final ExpenseRequest expenseRequest) {
        try {
            Integer countOfValidEmails = userService.checkIfAllEmailPresentInDb(expenseRequest.getParticipantEmails());
            if (countOfValidEmails != expenseRequest.getParticipantEmails().size()) {
                throw new IllegalArgumentException("All emails must be valid.");
            }

            final User paidBy = userService.getUserByEmail(expenseRequest.getPaidByUserEmail());
            final Set<User> participants = userService.getAllUsersByEmail(expenseRequest.getParticipantEmails());

            if (!participants.contains(paidBy)) {
                throw new IllegalArgumentException("Payer must be a participant in the expense.");
            }

            final Expense savedExpense = getExpense(expenseRequest, paidBy, participants);


            final BigDecimal totalAmount = savedExpense.getAmount();
            final int totalParticipants = participants.size();
            final BigDecimal perPersonShare = totalAmount.divide(BigDecimal.valueOf(totalParticipants), 2, RoundingMode.HALF_UP);

            createTransactions(savedExpense, perPersonShare);

            final Set<ParticipantResponse> participantResponses = getParticipantResponses(savedExpense, totalAmount, perPersonShare);

            final ExpenseResponse response = new ExpenseResponse();
            response.setId(savedExpense.getId());
            response.setDescription(savedExpense.getDescription());
            response.setAmount(savedExpense.getAmount());
            response.setPaidBy(new UserResponse(paidBy.getName(), paidBy.getEmail()));
            response.setParticipants(participantResponses);

            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid expense request.");
        }
    }

    public UserExpenseSummaryResponse getExpensesByUser(String email, boolean showParticipants, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Fetch expenses where the user is involved (either paid or is a participant)
        Page<Expense> expenses = expenseRepository.findByPaidByOrParticipantsContaining(user, user, pageable);

        AtomicReference<BigDecimal> netBalance = new AtomicReference<>(BigDecimal.ZERO);

        Page<ExpenseResponse> expenseResponses = expenses.map(expense -> {
            ExpenseResponse response = new ExpenseResponse();
            response.setId(expense.getId());
            response.setDescription(expense.getDescription());
            response.setAmount(expense.getAmount());
            response.setPaidBy(new UserResponse(expense.getPaidBy().getName(), expense.getPaidBy().getEmail()));
            response.setCreatedAt(expense.getCreatedAt());

            BigDecimal totalAmount = expense.getAmount();
            int numberOfParticipants = expense.getParticipants().size();
            BigDecimal individualShare = totalAmount.divide(BigDecimal.valueOf(numberOfParticipants), 2, RoundingMode.HALF_UP);

            BigDecimal netTransactionShare;

            if (user.equals(expense.getPaidBy())) {
                // If the user paid, they should receive money from others
                netTransactionShare = totalAmount.subtract(individualShare);
            } else {
                // If the user is a participant, they owe their share
                netTransactionShare = individualShare.negate();
            }

            response.setNetTransactionBalance(netTransactionShare);
            // Accumulate netBalance
            netBalance.updateAndGet(balance -> balance.add(netTransactionShare));

            // Accumulate netBalance
            netBalance.updateAndGet(balance -> balance.add(netTransactionShare));

            // Only calculate participant breakdown if showParticipants is true
            if (showParticipants) {
                Set<ParticipantResponse> participantResponses = getParticipantResponses(expense);

                response.setParticipants(participantResponses);
            }

            return response;
        });
        return new UserExpenseSummaryResponse(netBalance.get(), expenseResponses);
    }

    private Set<ParticipantResponse> getParticipantResponses(Expense expense) {
        Set<ParticipantResponse> participantResponses = new HashSet<>();
        BigDecimal totalAmount = expense.getAmount();
        int numberOfParticipants = expense.getParticipants().size();

        // Calculate individual share
        BigDecimal individualShare = totalAmount
                .divide(BigDecimal.valueOf(numberOfParticipants), 2, RoundingMode.HALF_UP);

        for (User participant : expense.getParticipants()) {
            BigDecimal amountOwed;
            BigDecimal amountToReceive;

            if (participant.equals(expense.getPaidBy())) {
                // Payer does not owe anything but will receive the remaining balance
                amountOwed = BigDecimal.ZERO;
                amountToReceive = totalAmount.subtract(individualShare);
            } else {
                // Other participants owe their share and do not receive anything
                amountOwed = individualShare;
                amountToReceive = BigDecimal.ZERO;
            }

            participantResponses.add(new ParticipantResponse(
                    participant.getName(),
                    participant.getEmail(),
                    amountOwed,
                    amountToReceive
            ));
        }
        return participantResponses;
    }

    private Set<ParticipantResponse> getParticipantResponses(Expense expense, BigDecimal totalAmount, BigDecimal perPersonShare) {
        return expense.getParticipants().stream().map(user -> {
            ParticipantResponse participant = new ParticipantResponse();
            participant.setName(user.getName());
            participant.setEmail(user.getEmail());

            if (user.equals(expense.getPaidBy())) {
                participant.setAmountOwed(BigDecimal.ZERO);
                participant.setAmountToReceive(totalAmount.subtract(perPersonShare));
            } else {
                participant.setAmountOwed(perPersonShare);
                participant.setAmountToReceive(BigDecimal.ZERO);
            }

            return participant;
        }).collect(Collectors.toSet());
    }


    private void createTransactions(Expense expense, BigDecimal perPersonShare) {
        Set<Transaction> transactions = new HashSet<>();

        for (User participant : expense.getParticipants()) {
            if (!participant.equals(expense.getPaidBy())) {
                Transaction transaction = Transaction.builder()
                        .fromUser(participant)
                        .toUser(expense.getPaidBy())
                        .amount(perPersonShare)
                        .expense(expense)
                        .build();
                transactions.add(transaction);
            }
        }

        transactionRepository.saveAll(transactions);
    }

    private Expense getExpense(ExpenseRequest expenseRequest, User paidBy, Set<User> participants) {
        Expense expense = new Expense();
        expense.setDescription(expenseRequest.getDescription());
        expense.setAmount(expenseRequest.getAmount());
        expense.setPaidBy(paidBy);
        expense.setParticipants(participants);

        return expenseRepository.save(expense);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }
}
