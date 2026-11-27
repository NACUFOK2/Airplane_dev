package com.airplane.domain;

public record ChatTurn(Role role, String content) {
    public enum Role { USER, ASSISTANT }
}
