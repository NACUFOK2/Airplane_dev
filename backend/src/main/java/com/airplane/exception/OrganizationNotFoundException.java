package com.airplane.exception;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(String code) {
        super("지원하지 않는 기관입니다: " + code);
    }
}
