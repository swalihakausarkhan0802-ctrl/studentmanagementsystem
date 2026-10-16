package com.studbuddy.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler.java — Centralized Error Handling
 *
 * PROBLEM WITHOUT THIS CLASS:
 * If StudentService throws a StudentNotFoundException and nobody catches it,
 * Spring Boot returns a generic 500 Internal Server Error with a Java stack trace.
 * That's ugly, leaks internal details, and confuses API clients.
 *
 * SOLUTION — @RestControllerAdvice:
 * This class "watches" all controllers.
 * When any controller throws an exception, Spring checks:
 * "Does GlobalExceptionHandler know how to handle this exception type?"
 * If yes → runs our @ExceptionHandler method → returns our clean JSON response.
 * If no  → falls back to Spring's default behavior.
 *
 * Think of it as a "catch block for the entire application."
 *
 * DIFFERENCE BETWEEN APPLICATION VALIDATION AND DATABASE CONSTRAINT:
 * - Application validation (@NotBlank, @Email):
 *   Checked BEFORE hitting the database. Fast. Returns 400 with friendly messages.
 *   Example: "email cannot be blank."
 *
 * - Database constraint (UNIQUE on email column):
 *   Checked WHEN Hibernate runs the SQL. Slower. Returns a cryptic SQL error.
 *   We catch DataIntegrityViolationException here to return 409 instead of 500.
 *   This is a SAFETY NET — our service-level check should catch duplicates first.
 */

@RestControllerAdvice
// ↑ Combination of @ControllerAdvice + @ResponseBody.
//   @ControllerAdvice → applies to all controllers in the application.
//   @ResponseBody     → exception handler return values are serialized to JSON.

public class GlobalExceptionHandler {

    // ================================================================
    // HANDLER 1: Student Not Found → 404 Not Found
    // ================================================================
    @ExceptionHandler(StudentNotFoundException.class)
    // ↑ This method runs whenever ANY controller throws StudentNotFoundException.
    //   Spring intercepts the exception BEFORE it becomes a 500 error.

    public ResponseEntity<Map<String, Object>> handleStudentNotFound(StudentNotFoundException ex) {
        // ↑ ResponseEntity<T> lets us control the HTTP status code AND the response body.
        //   Without ResponseEntity, Spring always returns 200 OK even for errors.
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)       // HTTP 404
                .body(buildError(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    // ================================================================
    // HANDLER 2: Duplicate Email → 409 Conflict
    // ================================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)        // HTTP 409
                .body(buildError(HttpStatus.CONFLICT, ex.getMessage()));
    }

    // ================================================================
    // HANDLER 3: Validation Failures (@NotBlank, @Email, @Min, @Max) → 400 Bad Request
    // ================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // ↑ Spring throws MethodArgumentNotValidException when @Valid finds violations.
    //   This gives us access to all the field-level validation errors.

    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Collect all field errors into a map: { "name": "Name must not be blank", ... }
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // ↑ ex.getBindingResult().getFieldErrors() returns a list of all violations.
            //   FieldError has:
            //     .getField()          → the field name ("name", "email", etc.)
            //     .getDefaultMessage() → the message from @NotBlank(message = "...")

            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = buildError(HttpStatus.BAD_REQUEST, "Validation failed");
        body.put("errors", fieldErrors);
        // ↑ We add the specific field errors to the response body.
        //   Example response:
        //   {
        //     "status": 400,
        //     "error": "Bad Request",
        //     "message": "Validation failed",
        //     "errors": {
        //       "name": "Name must not be blank",
        //       "email": "Email must be a valid email address"
        //     },
        //     "timestamp": "2026-10-13T..."
        //   }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ================================================================
    // HANDLER 4: Database Constraint Violation (safety net) → 409 Conflict
    // ================================================================
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex) {
        // This catches the MySQL unique constraint violation if it slips past our service check.
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError(HttpStatus.CONFLICT,
                        "A record with the same unique field (e.g., email) already exists."));
    }

    // ================================================================
    // HANDLER 5: Bad Credentials (wrong password / unknown email) → 401 Unauthorized
    // ================================================================
    @ExceptionHandler(BadCredentialsException.class)
    // ↑ AuthService.login() throws BadCredentialsException when:
    //   - The email doesn't exist in the database
    //   - The password doesn't match the stored BCrypt hash
    //   Both cases return the SAME 401 response — never reveal which one failed.
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)            // HTTP 401
                .body(buildError(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        // ↑ Generic message: don't tell the client whether the email or the password was wrong.
        //   This prevents "email enumeration" attacks where an attacker probes which
        //   email addresses have registered accounts.
    }

    // ================================================================
    // HELPER: Build a consistent error response body
    // ================================================================
    private Map<String, Object> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());           // e.g., 404
        body.put("error", status.getReasonPhrase());  // e.g., "Not Found"
        body.put("message", message);                 // e.g., "Student not found with id: 5"
        body.put("timestamp", LocalDateTime.now());   // when the error occurred
        return body;
    }
}
