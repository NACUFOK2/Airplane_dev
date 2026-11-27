package com.airplane.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getDefaultMessage()).orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", msg));
    }

    @ExceptionHandler(AirlineNotFoundException.class)
    public ResponseEntity<ApiError> handleAirline(AirlineNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("AIRLINE_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception e) {
        // 사용자 입력(개인정보 가능성)은 로그에 남기지 않고 예외 유형만 기록
        log.error("Unhandled error: {}", e.getClass().getSimpleName(), e);
        return ResponseEntity.internalServerError()
                .body(new ApiError("INTERNAL_ERROR", "일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요."));
    }
}
