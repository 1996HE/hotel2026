package com.example.minshuku.service;

/** Indicates a stale update or a conflicting room-inventory allocation. */
public class InventoryConflictException extends RuntimeException {
    public InventoryConflictException(String message) {
        super(message);
    }
}
