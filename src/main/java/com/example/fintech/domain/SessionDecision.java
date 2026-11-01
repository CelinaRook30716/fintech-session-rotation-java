package com.example.fintech.domain;

public record SessionDecision(Action action, String reason) {
    public enum Action { KEEP_SESSION, REVOKE_SESSION }
}
