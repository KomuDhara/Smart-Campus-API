package com.smartcampus.exception;

// ── 409 Conflict ─────────────────────────────────────────────────────────────
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) { super(message); }
}
