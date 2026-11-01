package com.example.fintech;

import com.example.fintech.application.PaymentSessionService;
import com.example.fintech.domain.PaymentEvent;
import com.example.fintech.domain.SessionDecision;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class SessionPolicyTest {
    public static void main(String[] args) {
        List<String> revoked = new ArrayList<>();
        List<String> audit = new ArrayList<>();
        PaymentSessionService service = new PaymentSessionService(revoked::add,
                (event, decision) -> audit.add(event.sessionId() + ":" + decision.reason()),
                new BigDecimal("1000.00"));
        SessionDecision decision = service.handle(
                new PaymentEvent("session-42", "user-7", new BigDecimal("1200.00"), "device-new"),
                "device-old");
        assert decision.action() == SessionDecision.Action.REVOKE_SESSION;
        assert revoked.equals(List.of("session-42"));
        assert audit.equals(List.of("session-42:HIGH_VALUE_DEVICE_CHANGE"));
        System.out.println("PASS: high-value device change revokes session and records audit event");
    }
}
