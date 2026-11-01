package com.example.fintech.domain;

import java.math.BigDecimal;

public record PaymentEvent(String sessionId, String userId, BigDecimal amount, String deviceFingerprint) {
}
