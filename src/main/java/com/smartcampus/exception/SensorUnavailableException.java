package com.smartcampus.exception;

// ── 403 Forbidden ─────────────────────────────────────────────────────────────
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) { super(message); }
}
