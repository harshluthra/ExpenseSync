package org.example.service;

import org.example.dto.*;
import org.example.exception.ExpenseSyncException;
import org.example.model.Expense;
import org.example.model.ExpenseParticipant;
import org.example.model.User;
import org.example.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

class ExpenseServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateExpense_withEqualSplit_success() {
        User payer = new User("krish", "krish@example.com");
        User janhvi = new User("Janhvi", "janhvi@example.com");
        User harsh = new User("Harsh", "harsh@example.com");

        List<ParticipantDTO> participants = Arrays.asList(
                new ParticipantDTO("krish@example.com", null),
                new ParticipantDTO("janhvi@example.com", null),
                new ParticipantDTO("harsh@example.com", null)
        );

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Dinner")
                .amount(BigDecimal.valueOf(1500))
                .paidByEmail("krish@example.com")
                .splitType(SplitType.EQUAL)
                .participants(participants)
                .build();

        Set<User> users = new HashSet<>(Arrays.asList(payer, janhvi, harsh));

        when(userService.getUserByEmail("krish@example.com")).thenReturn(payer);
        when(userService.getAllUsersByEmail(anySet())).thenReturn(users);
        when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateExpenseResponse response = expenseService.createExpense(request);

        assertNotNull(response);
        assertEquals("Dinner", response.getDescription());
        assertEquals(BigDecimal.valueOf(1500), response.getAmount());
        assertEquals("krish@example.com", response.getPaidBy().get("email"));
        assertEquals(3, response.getParticipants().size());
    }

    @Test
    void testCreateExpense_missingPayer_throwsException() {
        List<ParticipantDTO> participants = Arrays.asList(
                new ParticipantDTO("janhvi@example.com", null),
                new ParticipantDTO("harsh@example.com", null)
        );

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Dinner")
                .amount(BigDecimal.valueOf(1000))
                .paidByEmail("krish@example.com")
                .splitType(SplitType.EQUAL)
                .participants(participants)
                .build();

        assertThrows(ExpenseSyncException.class, () -> expenseService.createExpense(request));
    }

    @Test
    void testCreateExpense_withExactSplitMismatch_throwsException() {
        List<ParticipantDTO> participants = Arrays.asList(
                new ParticipantDTO("krish@example.com", BigDecimal.valueOf(500)),
                new ParticipantDTO("janhvi@example.com", BigDecimal.valueOf(400)),
                new ParticipantDTO("harsh@example.com", BigDecimal.valueOf(200))
        );

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .description("Taxi")
                .amount(BigDecimal.valueOf(1200))
                .paidByEmail("krish@example.com")
                .splitType(SplitType.EXACT)
                .participants(participants)
                .build();

        Set<User> users = new HashSet<>(Arrays.asList(
                new User("krish", "krish@example.com"),
                new User("janhvi", "janhvi@example.com"),
                new User("harsh", "harsh@example.com")
        ));

        when(userService.getUserByEmail("krish@example.com")).thenReturn(users.iterator().next());
        when(userService.getAllUsersByEmail(anySet())).thenReturn(users);

        assertThrows(ExpenseSyncException.class, () -> expenseService.createExpense(request));
    }

    @Test
    void testGetExpensesByUserEmail_returnsCorrectSummary() {
        String email = "krish@example.com";
        User krish = new User("Krish", email);
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDescription("Dinner");
        expense.setAmount(BigDecimal.valueOf(900));
        expense.setCreatedAt(LocalDateTime.now());
        expense.setPaidBy(krish);

        ExpenseParticipant ep = new ExpenseParticipant();
        ep.setUser(krish);
        ep.setExpense(expense);
        ep.setShareAmount(BigDecimal.valueOf(300));

        expense.setParticipants(List.of(ep));

        when(userService.getUserByEmail(email)).thenReturn(krish);
        when(expenseRepository.findAllByParticipantsUserEmail(email)).thenReturn(List.of(expense));

        UserExpenseSummary summary = expenseService.getExpensesByUserEmail(email, true);

        assertNotNull(summary);
        assertEquals(1, summary.getExpenses().size());
        assertEquals(BigDecimal.valueOf(600), summary.getNetBalance());
    }
}
