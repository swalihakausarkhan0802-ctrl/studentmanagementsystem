package com.studbuddy.backend.repository;
// ↑ This file lives in the "repository" sub-package.

import com.studbuddy.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
// ↑ Optional<T> is a container that may or may not hold a value.
//   It forces us to consciously handle the "not found" case.
//   Like: Optional<Student> = either a Student object, or empty (not null).

/**
 * StudentRepository.java — The Data Access Layer
 *
 * WHAT IS AN INTERFACE IN JAVA?
 * An interface is a CONTRACT — it defines WHAT methods exist, but not HOW they work.
 * The "how" is provided by whoever IMPLEMENTS the interface.
 *
 * Example:
 *   interface Animal { void speak(); }           ← contract: animals can speak
 *   class Dog implements Animal { void speak() { System.out.println("Woof"); } }
 *   class Cat implements Animal { void speak() { System.out.println("Meow"); } }
 *
 * WHAT IS JpaRepository?
 * JpaRepository<Student, Long> is a Spring interface that already knows how to:
 *   - save(student)          → INSERT or UPDATE in MySQL
 *   - findAll()              → SELECT * FROM students
 *   - findById(id)           → SELECT * FROM students WHERE id = ?
 *   - deleteById(id)         → DELETE FROM students WHERE id = ?
 *   - existsById(id)         → SELECT COUNT(*) ... WHERE id = ?
 *   - count()                → SELECT COUNT(*) FROM students
 *   ... and many more.
 *
 * You write ZERO SQL. Spring Data JPA generates all of these implementations
 * automatically at application startup by creating a proxy class for you.
 *
 * WHY DO WE EXTEND JpaRepository<Student, Long>?
 * The two generic type parameters tell Spring:
 *   Student → this repository manages Student entities
 *   Long    → the primary key type is Long (matches "private Long id" in Student.java)
 *
 * If you had a User entity with "private Integer id", you'd write JpaRepository<User, Integer>.
 */

@Repository
// ↑ Marks this interface as a Spring-managed repository.
//   Optional here (Spring Data JPA detects it anyway), but good practice for clarity.
//   It also enables Spring to translate low-level database exceptions into
//   Spring's DataAccessException hierarchy.

public interface StudentRepository extends JpaRepository<Student, Long> {
    // ↑ "extends" means we INHERIT all methods from JpaRepository.
    //   We get save, findAll, findById, deleteById, etc. for FREE.
    //   Spring creates the implementation class at startup — you never see it.

    // ----------------------------------------------------------------
    // CUSTOM QUERY METHOD
    // ----------------------------------------------------------------
    //
    // For the unique email check, we need to:
    //   1. Find a student by email (before creating, to detect duplicates).
    //   2. Check if an email already exists (before updating).
    //
    // Spring Data JPA can generate queries from METHOD NAMES automatically!
    // The method name "findByEmail" tells Spring:
    //   "SELECT * FROM students WHERE email = ?"
    // No @Query annotation needed. Spring reads the method name and generates the SQL.
    //
    // Return type: Optional<Student>
    //   - If found: Optional.of(student) — contains the student
    //   - If not found: Optional.empty() — no student with that email
    //   Using Optional forces us to handle both cases, preventing NullPointerExceptions.

    Optional<Student> findByEmail(String email);
    // ↑ Spring translates "findByEmail" → "SELECT * FROM students WHERE email = ?"
    //   This is called a "derived query" or "query method."
    //   Other examples Spring can auto-generate:
    //     findByName(String name)       → WHERE name = ?
    //     findByBranch(String branch)   → WHERE branch = ?
    //     findByYear(Integer year)      → WHERE year = ?
    //     findByNameAndBranch(...)      → WHERE name = ? AND branch = ?

    boolean existsByEmail(String email);
    // ↑ Returns true if ANY student has this email, false otherwise.
    //   Translates to: SELECT COUNT(*) > 0 FROM students WHERE email = ?
    //   We'll use this in the service layer to check for duplicate emails
    //   before attempting an insert.
}
