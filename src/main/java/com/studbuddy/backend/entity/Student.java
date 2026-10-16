package com.studbuddy.backend.entity;
// ↑ This file lives in the "entity" sub-package.
//   Physical path: src/main/java/com/studbuddy/backend/entity/Student.java
//
//   WHAT IS A PACKAGE?
//   A package is like a folder for organizing Java classes.
//   com.studbuddy.backend.entity = we're in the "entity" folder inside our project.

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
// ↑ These are JPA annotations — they tell Hibernate how to map this class to MySQL.
//   "jakarta.persistence" is the modern package name (Spring Boot 3.x uses Jakarta EE 10).
//   In older Spring Boot 2.x projects you'd see "javax.persistence" instead.

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
// ↑ Bean Validation annotations.
//   These validate the data BEFORE Hibernate tries to save it to the database.
//   Think of them as a "bouncer at the door" — bad data gets rejected early.

/**
 * Student.java — The Entity Class
 *
 * WHAT IS AN ENTITY?
 * An "entity" is a Java class that represents a table in the database.
 * Each field in this class maps to a column in that table.
 * Each object (instance) of this class represents one ROW in that table.
 *
 * Analogy:
 *   Java class Student     ↔  MySQL table  "students"
 *   Student object         ↔  one row in the table
 *   Student field "name"   ↔  the "name" column
 *
 * HOW DOES A JAVA OBJECT BECOME A DATABASE ROW?
 * Hibernate reads the annotations on this class and:
 *   1. Creates a "students" table (on first run with ddl-auto=update)
 *   2. When you call repository.save(student), Hibernate runs:
 *      INSERT INTO students (name, email, branch, year) VALUES (?, ?, ?, ?)
 *   3. When you call repository.findAll(), Hibernate runs:
 *      SELECT * FROM students
 *   You never write that SQL yourself!
 */

@Entity
// ↑ Marks this class as a JPA entity.
//   Without this, Hibernate ignores the class entirely.
//   With this, Hibernate knows: "this class = one database table."

@Table(name = "students")
// ↑ Specifies the exact table name in MySQL.
//   Without @Table, Hibernate would use the class name "Student" as the table name.
//   With @Table(name = "students"), the table will be named "students" (lowercase, plural).
//   This is a common convention: Java class is singular (Student), table is plural (students).

public class Student {
    // ↑ A plain Java class. The annotations above are what make it special.

    @Id
    // ↑ Marks this field as the PRIMARY KEY of the table.
    //   Every entity MUST have an @Id field.
    //   Primary key = the unique identifier for each row. No two students share the same id.

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ↑ Tells Hibernate: "Don't ask me for an id — the database will generate it automatically."
    //   GenerationType.IDENTITY means MySQL's AUTO_INCREMENT feature.
    //   So the "id" column in MySQL will be: INT AUTO_INCREMENT PRIMARY KEY
    //   When you insert a new student, MySQL automatically assigns id = 1, 2, 3, ...
    //   You never set the id manually.

    private Long id;
    // ↑ "Long" is a Java data type for whole numbers.
    //   It maps to BIGINT in MySQL — supports very large numbers (up to ~9.2 quintillion).
    //   We use Long (not int) because "private Long id" can also be null (before the row is saved),
    //   whereas "private long id" (lowercase, primitive) cannot be null.
    //   Database type: BIGINT AUTO_INCREMENT PRIMARY KEY

    @NotBlank(message = "Name must not be blank")
    // ↑ Bean Validation constraint: ensures the "name" field is not null AND not an empty/whitespace string.
    //   If someone sends { "name": "" }, validation fails and Spring returns 400 Bad Request.
    //   "message" is what gets included in the error response.

    @Column(name = "name", nullable = false, length = 100)
    // ↑ Maps this field to a column named "name" in the students table.
    //   nullable = false  → the column has a NOT NULL constraint in MySQL
    //   length = 100      → MySQL column type becomes VARCHAR(100)
    //   Without @Column, Hibernate uses the field name and default settings.

    private String name;
    // Database type: VARCHAR(100) NOT NULL

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid email address")
    // ↑ @Email checks that the value looks like a valid email (e.g., user@example.com).
    //   Both @NotBlank and @Email are checked together.

    @Column(name = "email", nullable = false, unique = true, length = 150)
    // ↑ unique = true → MySQL creates a UNIQUE constraint on this column.
    //   This means no two students can have the same email in the database.
    //   This is a DATABASE-LEVEL constraint — even if application code misses a duplicate,
    //   MySQL will reject it.
    //   We'll also check for duplicates in the service layer (application-level) to
    //   return a friendlier 409 Conflict response before hitting the database.

    private String email;
    // Database type: VARCHAR(150) NOT NULL UNIQUE

    @NotBlank(message = "Branch must not be blank")

    @Column(name = "branch", nullable = false, length = 100)
    private String branch;
    // Examples: "Computer Engineering", "Mechanical Engineering", "Civil Engineering"
    // Database type: VARCHAR(100) NOT NULL

    @NotNull(message = "Year must not be null")
    // ↑ @NotNull ensures the field is not null.
    //   We use @NotNull (not @NotBlank) here because "year" is a number, not a String.
    //   @NotBlank only works on Strings.

    @Min(value = 1, message = "Year must be at least 1")
    @Max(value = 4, message = "Year must be at most 4")
    // ↑ Engineering has 4 years. @Min and @Max enforce this range.
    //   If someone sends { "year": 0 } or { "year": 5 }, validation fails.

    @Column(name = "year", nullable = false)
    private Integer year;
    // ↑ "Integer" (capital I) is the object wrapper for "int".
    //   We use Integer (not int) because it can be null — which @NotNull will then catch.
    //   Database type: INT NOT NULL

    // ================================================================
    // CONSTRUCTORS
    // ================================================================
    //
    // WHAT IS A CONSTRUCTOR?
    // A constructor is a special method that runs when you create a new object.
    // Like: new Student()  or  new Student("Aarav", "aarav@example.com", ...)
    //
    // WHY TWO CONSTRUCTORS?
    // JPA (Hibernate) REQUIRES a no-argument constructor to create objects
    // when it reads rows from the database. If this is missing, Hibernate crashes.

    // 1. No-argument constructor (required by JPA/Hibernate)
    public Student() {
        // Hibernate uses this when it reads a row from the database.
        // It creates an empty Student object, then fills in the fields.
    }

    // 2. All-fields constructor (convenient for creating students in code/tests)
    public Student(String name, String email, String branch, Integer year) {
        // We don't include "id" here because the database generates it automatically.
        this.name = name;
        this.email = email;
        this.branch = branch;
        this.year = year;
    }

    // ================================================================
    // GETTERS AND SETTERS
    // ================================================================
    //
    // WHAT ARE GETTERS AND SETTERS?
    // In Java, fields are declared "private" — other classes cannot access them directly.
    // Getters and setters are PUBLIC methods that provide controlled access.
    //
    //   private String name;       ← only this class can access "name"
    //   public String getName()    ← other classes can READ the name
    //   public void setName(...)   ← other classes can WRITE the name
    //
    // Jackson (the JSON library) uses getters to convert your Student object to JSON.
    // Hibernate uses setters to fill in values when reading from the database.
    // Without getters, your API would return empty JSON: {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    // toString() — optional but useful for debugging.
    // When you print a Student object in logs, this is what you'll see.
    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", branch='" + branch + '\'' +
                ", year=" + year +
                '}';
    }
}
