package com.example.fintech.application;

import com.example.fintech.domain.PaymentEvent;
import com.example.fintech.domain.SessionDecision;
import java.math.BigDecimal;

public final class PaymentSessionService {
    private final SessionGateway sessions;
    private final AuditNotifier audit;
    private final BigDecimal reviewAmount;

    public PaymentSessionService(SessionGateway sessions, AuditNotifier audit, BigDecimal reviewAmount) {
        this.sessions = sessions;
        this.audit = audit;
        this.reviewAmount = reviewAmount;
    }

    public SessionDecision handle(PaymentEvent event, String lastDeviceFingerprint) {
        boolean changedDevice = !event.deviceFingerprint().equals(lastDeviceFingerprint);
        boolean needsReview = event.amount().compareTo(reviewAmount) >= 0 && changedDevice;
        SessionDecision decision = needsReview
                ? new SessionDecision(SessionDecision.Action.REVOKE_SESSION, "HIGH_VALUE_DEVICE_CHANGE")
                : new SessionDecision(SessionDecision.Action.KEEP_SESSION, "PAYMENT_ACCEPTED");
        if (decision.action() == SessionDecision.Action.REVOKE_SESSION) {
            sessions.revoke(event.sessionId());
        }
        audit.record(event, decision);
        return decision;
    }

    public interface SessionGateway {
        void revoke(String sessionId);
    }

    public interface AuditNotifier {
        void record(PaymentEvent event, SessionDecision decision);
    }
}
