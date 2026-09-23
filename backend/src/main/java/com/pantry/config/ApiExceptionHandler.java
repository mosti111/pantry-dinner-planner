package com.pantry.config;

import com.pantry.identity.GuestSessionNotFoundException;
import com.pantry.planning.PlanNotFoundException;
import com.pantry.planning.IdempotencyConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.MDC;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(GuestSessionNotFoundException.class)
    ResponseEntity<ProblemDetail> guestSession(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "GUEST_SESSION_REQUIRED", "A valid guest session is required", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> invalid(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PLAN_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return problem(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED", detail, request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> conflict(ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "PLAN_VERSION_CONFLICT", "The plan changed; refresh and try again", request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ProblemDetail> idempotencyConflict(IdempotencyConflictException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(PlanNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(PlanNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", exception.getMessage(), request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://pantry.example/problems/" + code.toLowerCase().replace('_', '-')));
        problem.setProperty("code", code);
        String correlationId = MDC.get("correlationId");
        problem.setProperty("correlationId", correlationId == null ? UUID.randomUUID().toString() : correlationId);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }
}
