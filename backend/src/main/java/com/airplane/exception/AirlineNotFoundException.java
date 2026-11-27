package com.airplane.exception;

public class AirlineNotFoundException extends RuntimeException {
    public AirlineNotFoundException(String code) {
        super("지원하지 않는 항공사입니다: " + code);
    }
}
