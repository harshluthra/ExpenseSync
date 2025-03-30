package org.example.exception;

public class ExpenseSyncException extends RuntimeException {
    public ExpenseSyncException(String message) {
        super(message);
    }
}
