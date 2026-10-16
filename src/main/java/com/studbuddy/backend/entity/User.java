package com.studbuddy.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User.java — The User Entity
 *
 * This represents a registered user in our system.
 * It maps to a "users" table in MySQL (separate from the "students" table).
 *
 * DIFFERENCE BETWEEN User AND Student:
 *   Student = a college student RECORD (name, branch, year) — the data we manage
 *   User    = an APP USER who logs in to manage those records
 *
 * In Phase 4, we only need one role (USER) for all app users.
 * Role-based access control (ADMIN vs USER) is intentionally out of scope.
 *
 * WHY DOESN'T User IMPLEMENT UserDetails?
 * Spring Security has a UserDetails interface that represents a logged-in user.
 * We keep User clean (plain JPA entity) and let AuthService adapt it to
 * Spring Security's needs. This keeps database code and security code separate.
 */

@Entity
@Table(name = "users")
// ↑ Maps to the "users" table. NOT "user" — that's a reserved word in MySQL.
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name must not be blank")
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    // e.g. "Aarav Sharma"

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;
    // Used as the login username. Must be unique across all users.

    @NotBlank
    @Size(min = 60, max = 60)
    // ↑ BCrypt always produces exactly 60 characters.
    //   @Size ensures we never accidentally store a plain-text password
    //   that's shorter than 60 chars (which would indicate a coding bug).
    @Column(name = "password", nullable = false, length = 60)
    private String password;
    // NEVER the plain-text password. Always a BCrypt hash like:
    // $2a$10$X7kJq3BrqtM8TjE2.xPNiOpb4b0R8P...

    @Column(name = "role", nullable = false, length = 20)
    private String role = "USER";
    // Every new user gets the "USER" role by default.
    // The public registration endpoint CANNOT set this to "ADMIN".
    // Default value here ensures it's always set even if not explicitly assigned.

    // ---- Constructors ----

    public User() {
        // Required by JPA/Hibernate
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = "USER";
    }

    // ---- Getters & Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        // IMPORTANT: never include the password field in toString() to avoid
        // accidentally printing it in logs.
        return "User{id=" + id + ", name='" + name + "', email='" + email + "', role='" + role + "'}";
    }
}
