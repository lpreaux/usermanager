package fr.lpreaux.usermanager.infrastructure.adapter.in.web.exception;

import fr.lpreaux.usermanager.application.exception.*;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers with Sentry integration.
 * This class captures exceptions, logs them, and sends them to Sentry for monitoring.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Utility method to create a ProblemDetail with consistent structure.
     */
    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    /**
     * Handle user not found exceptions.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        log.warn("User not found: {}", ex.getMessage());

        // Capture in Sentry as INFO level (not critical error)
        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.INFO);
            scope.setTag("error_type", "user_not_found");
            scope.setTag("request_uri", request.getDescription(false));
            scope.setExtra("user_id", ex.getMessage().replaceAll(".*ID: ", ""));
            Sentry.captureException(ex);
        });

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), request);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handle user already exists exceptions.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, WebRequest request) {
        log.warn("User already exists: {}", ex.getMessage());

        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.WARNING);
            scope.setTag("error_type", "user_already_exists");
            scope.setTag("request_uri", request.getDescription(false));
            // Extract login or email from the exception message if possible
            if (ex.getMessage().contains("Login already exists:")) {
                scope.setExtra("login", ex.getMessage().replaceAll("Login already exists: ", ""));
            } else if (ex.getMessage().contains("Email already exists:")) {
                scope.setExtra("email", ex.getMessage().replaceAll("Email already exists: ", ""));
            }
            Sentry.captureException(ex);
        });

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT, "User Already Exists", ex.getMessage(), request);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Handle email already exists exceptions.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex, WebRequest request) {
        log.warn("Email already exists: {}", ex.getMessage());

        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.WARNING);
            scope.setTag("error_type", "email_already_exists");
            scope.setTag("request_uri", request.getDescription(false));
            scope.setExtra("email", ex.getMessage().replaceAll("Email already exists: ", ""));
            Sentry.captureException(ex);
        });

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT, "Email Already Exists", ex.getMessage(), request);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Handle invalid password exceptions.
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPasswordException(
            InvalidPasswordException ex, WebRequest request) {
        log.warn("Invalid password: {}", ex.getMessage());

        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.WARNING);
            scope.setTag("error_type", "invalid_password");
            scope.setTag("request_uri", request.getDescription(false));
            Sentry.captureException(ex);
        });

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST, "Invalid Password", ex.getMessage(), request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handle last email exceptions.
     */
    @ExceptionHandler(LastEmailException.class)
    public ResponseEntity<ProblemDetail> handleLastEmailException(
            LastEmailException ex, WebRequest request) {
        log.warn("Cannot remove last email: {}", ex.getMessage());

        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.WARNING);
            scope.setTag("error_type", "last_email_deletion");
            scope.setTag("request_uri", request.getDescription(false));
            Sentry.captureException(ex);
        });

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST, "Cannot Remove Last Email", ex.getMessage(), request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.WARNING);
            scope.setTag("error_type", "validation_error");
            scope.setTag("request_uri", request.getDescription(false));
            scope.setExtra("field_errors", errors.toString());
            // Add each field error as a separate breadcrumb
            errors.forEach((field, message) -> {
                Sentry.addBreadcrumb("Validation error on field '" + field + "': " + message);
            });
            Sentry.captureException(ex);
        });

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST, "Validation Error", "The request contains invalid fields", request);

        problemDetail.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handle IllegalArgumentException (often thrown by Value Objects).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());

        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.WARNING);
            scope.setTag("error_type", "invalid_argument");
            scope.setTag("request_uri", request.getDescription(false));
            // Add stack trace to help identify which value object caused the exception
            scope.setExtra("stack_trace_element", ex.getStackTrace().length > 0 ?
                    ex.getStackTrace()[0].toString() : "Unknown");
            Sentry.captureException(ex);
        });

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage(), request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handle any other exceptions (fallback).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);

        // Capture in Sentry as a critical ERROR
        Sentry.withScope(scope -> {
            scope.setLevel(SentryLevel.ERROR);
            scope.setTag("error_type", "unexpected");
            scope.setTag("request_uri", request.getDescription(false));
            scope.setExtra("exception_class", ex.getClass().getName());

            // Add stack trace information as breadcrumbs for better debugging
            int breadcrumbLimit = Math.min(ex.getStackTrace().length, 10); // Limit to 10 entries
            for (int i = 0; i < breadcrumbLimit; i++) {
                Sentry.addBreadcrumb(ex.getStackTrace()[i].toString());
            }

            Sentry.captureException(ex);
        });

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}