package com.studbuddy.backend.dto;

/**
 * AuthResponse.java — What we send back after registration or login
 *
 * After login: { "token": "eyJhbGc...", "email": "aarav@example.com", "name": "Aarav" }
 * After register: { "message": "Registration successful" }
 *
 * WHY NOT RETURN THE FULL User OBJECT?
 * The User entity contains the BCrypt password hash — we must never send it to clients.
 * This DTO lets us precisely control what goes in the response.
 *
 * NOTE: A JWT is SIGNED, not ENCRYPTED.
 * Anyone can decode the payload (base64 decode it) and read the claims.
 * That's why we only put non-sensitive info in it (email, expiry).
 * We NEVER put the password or password hash inside the JWT.
 */
public class AuthResponse {

    private String token;
    // The JWT access token. Client must send this in the Authorization header.
    // Format: "Authorization: Bearer eyJhbGc..."

    private String email;
    // The logged-in user's email — for the frontend to display

    private String name;
    // The logged-in user's name — for the frontend to display

    private String message;
    // Optional message (e.g., "Registration successful" or "Login successful")

    public AuthResponse() {}

    // Constructor for login response (includes token)
    public AuthResponse(String token, String email, String name, String message) {
        this.token = token;
        this.email = email;
        this.name = name;
        this.message = message;
    }

    // Constructor for register response (no token needed yet)
    public AuthResponse(String message) {
        this.message = message;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
