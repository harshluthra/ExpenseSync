package org.example.service;

import org.example.dto.RawBalanceResponse;
import org.example.dto.RawTransaction;
import org.example.dto.SimplifiedBalanceResponse;
import org.example.dto.SimplifiedTransaction;
import org.example.model.Expense;
import org.example.model.ExpenseParticipant;
import org.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BalanceServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private BalanceService balanceService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetRawBalance_returnsCorrectTransactions() {
        User krish = new User("Krish", "krish@example.com");
        User janhvi = new User("Janhvi", "janhvi@example.com");

        Expense expense = new Expense();
        expense.setDescription("Snacks");
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setPaidBy(krish);
        expense.setCreatedAt(LocalDateTime.now());

        ExpenseParticipant p1 = new ExpenseParticipant();
        p1.setUser(krish);
        p1.setExpense(expense);
        p1.setShareAmount(BigDecimal.valueOf(50));

        ExpenseParticipant p2 = new ExpenseParticipant();
        p2.setUser(janhvi);
        p2.setExpense(expense);
        p2.setShareAmount(BigDecimal.valueOf(50));

        expense.setParticipants(List.of(p1, p2));

        when(userService.getUserByEmail("krish@example.com")).thenReturn(krish);
        when(userService.getUserByEmail("janhvi@example.com")).thenReturn(janhvi);
        when(expenseService.getExpensesByUserEmail("krish@example.com")).thenReturn(List.of(expense));

        RawBalanceResponse response = balanceService.getRawBalance("krish@example.com");

        assertNotNull(response);
        assertEquals("krish@example.com", response.getUser().get("email"));
        assertEquals(1, response.getTransactions().size());

        RawTransaction txn = response.getTransactions().get(0);
        assertEquals("janhvi@example.com", txn.getFrom().get("email"));
        assertEquals("krish@example.com", txn.getTo().get("email"));
        assertEquals(BigInteger.valueOf(50), txn.getAmount().toBigInteger());
    }

    @Test
    void testGetSimplifiedBalance_returnsOptimizedTransactions() {
        User krish = new User("Krish", "krish@example.com");
        User janhvi = new User("Janhvi", "janhvi@example.com");
        User harsh = new User("Harsh", "harsh@example.com");

        Expense expense1 = new Expense();
        expense1.setDescription("Lunch");
        expense1.setAmount(BigDecimal.valueOf(1500));
        expense1.setPaidBy(krish);
        expense1.setCreatedAt(LocalDateTime.now());

        ExpenseParticipant ep1 = new ExpenseParticipant();
        ep1.setUser(krish);
        ep1.setShareAmount(BigDecimal.valueOf(500));

        ExpenseParticipant ep2 = new ExpenseParticipant();
        ep2.setUser(janhvi);
        ep2.setShareAmount(BigDecimal.valueOf(500));

        ExpenseParticipant ep3 = new ExpenseParticipant();
        ep3.setUser(harsh);
        ep3.setShareAmount(BigDecimal.valueOf(500));

        expense1.setParticipants(List.of(ep1, ep2, ep3));

        when(expenseService.fetchAllExpenses()).thenReturn(List.of(expense1));
        when(userService.getUserByEmail("krish@example.com")).thenReturn(krish);
        when(userService.getUserByEmail("janhvi@example.com")).thenReturn(janhvi);
        when(userService.getUserByEmail("harsh@example.com")).thenReturn(harsh);

        SimplifiedBalanceResponse response = balanceService.getSimplifiedBalance("krish@example.com");

        assertNotNull(response);
        assertEquals("krish@example.com", response.getUser().get("email"));
        assertEquals(2, response.getTransactions().size());

        BigDecimal total = response.getTransactions().stream()
                .map(SimplifiedTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(BigInteger.valueOf(1000), total.toBigInteger());
    }
}

