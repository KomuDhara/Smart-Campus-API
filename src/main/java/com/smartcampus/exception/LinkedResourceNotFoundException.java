package com.smartcampus.exception;

// ── 422 Unprocessable Entity ──────────────────────────────────────────────────
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) { super(message); }
}
