package com.studbuddy.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequest.java — Registration DTO
 *
 * WHAT IS A DTO?
 * DTO = Data Transfer Object.
 * It's a plain Java class that carries data between the client and the server.
 *
 * WHY NOT USE THE User ENTITY DIRECTLY?
 * If we accepted a User object in the register endpoint, the client could
 * send { "role": "ADMIN" } and give themselves admin privileges.
 * A DTO lets us accept ONLY the fields we want (name, email, password).
 * The role is always set to "USER" in the service — the client never controls it.
 *
 * This is called the "principle of least privilege" in API design.
 */
public class RegisterRequest {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    // ↑ @Size checks the length of the plain-text password BEFORE hashing.
    //   A minimum of 8 characters is a basic security requirement.
    //   After BCrypt hashing, the stored value is always 60 characters — unrelated.
    private String password;

    public RegisterRequest() {}

    public RegisterRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
