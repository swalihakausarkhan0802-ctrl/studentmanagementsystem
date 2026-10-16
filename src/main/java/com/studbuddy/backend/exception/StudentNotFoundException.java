package com.studbuddy.backend.exception;

/**
 * StudentNotFoundException.java
 *
 * WHAT IS AN EXCEPTION?
 * An exception is an error signal in Java.
 * When something goes wrong (e.g., student not found), we "throw" an exception.
 * The caller "catches" it and decides what to do (e.g., return 404).
 *
 * WHY A CUSTOM EXCEPTION?
 * We could use generic exceptions like RuntimeException("not found"),
 * but a custom exception makes the error reason crystal clear.
 * Our GlobalExceptionHandler can then detect this specific type and
 * return exactly a 404 response — not 400, not 500.
 *
 * "extends RuntimeException" means:
 * This IS a RuntimeException (unchecked), so callers don't have to
 * declare "throws StudentNotFoundException" in their method signatures.
 * This keeps our service and controller code cleaner.
 */
public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(Long id) {
        super("Student not found with id: " + id);
        // ↑ "super(...)" calls the parent class constructor (RuntimeException)
        //   and sets the error message. You can read it with .getMessage().
    }
}
