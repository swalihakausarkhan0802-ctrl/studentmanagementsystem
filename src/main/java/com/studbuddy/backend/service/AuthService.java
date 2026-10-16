package com.studbuddy.backend.service;

import com.studbuddy.backend.dto.AuthResponse;
import com.studbuddy.backend.dto.LoginRequest;
import com.studbuddy.backend.dto.RegisterRequest;
import com.studbuddy.backend.entity.User;
import com.studbuddy.backend.repository.UserRepository;
import com.studbuddy.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthService.java — Registration and Login Business Logic
 *
 * This service handles the two authentication operations:
 *   1. REGISTER: validate → hash password → save user → return success
 *   2. LOGIN: validate credentials → generate JWT → return token
 *
 * WHY IS PASSWORD HASHING DONE HERE, NOT IN THE CONTROLLER?
 * The controller only deals with HTTP. The decision to hash a password
 * is a business rule, so it belongs in the service layer.
 * Controllers should be thin — receive request, call service, return response.
 *
 * IMPORTANT SECURITY FLOWS:
 *
 * Registration:
 *   plain password ("ExamplePass123!")
 *        ↓
 *   passwordEncoder.encode("ExamplePass123!")
 *        ↓
 *   BCrypt hash ("$2a$10$X7k...")  → saved to MySQL
 *   (original password is NEVER stored)
 *
 * Login:
 *   email + plain password ("ExamplePass123!")
 *        ↓
 *   authenticationManager.authenticate(...)
 *        ↓  (Spring finds user by email, calls BCrypt.matches(plain, hash))
 *   If match → authentication SUCCESS → generate JWT
 *   If no match → BadCredentialsException → return 401
 */

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    // ================================================================
    // REGISTER
    // ================================================================

    /**
     * Registers a new user.
     *
     * @param request validated DTO from the controller
     * @return AuthResponse with a success message
     * @throws IllegalArgumentException if the email is already registered
     */
    public AuthResponse register(RegisterRequest request) {
        // Step 1: Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "An account with email '" + request.getEmail() + "' already exists."
            );
            // ↑ IllegalArgumentException → caught by GlobalExceptionHandler → 409 Conflict
        }

        // Step 2: Hash the password using BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        // ↑ passwordEncoder is a BCryptPasswordEncoder (configured in SecurityConfig).
        //   .encode("ExamplePass123!") → "$2a$10$X7kJq3..."
        //   This takes ~100ms intentionally — BCrypt is designed to be slow.
        //   The original password is never stored anywhere.

        // Step 3: Create the User entity (with hashed password, role defaults to "USER")
        User user = new User(request.getName(), request.getEmail(), hashedPassword);
        // ↑ We pass the HASHED password, not the original.
        //   The User constructor sets role = "USER" automatically.
        //   The client's RegisterRequest DTO has no role field — clients cannot set it.

        // Step 4: Save to MySQL
        userRepository.save(user);
        // ↑ Hibernate runs:
        //   INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)
        //   where the password value is the BCrypt hash string.

        // Step 5: Return success (no token yet — user must log in separately)
        return new AuthResponse("Registration successful. Please log in.");
        // ↑ We deliberately don't include the password or hash in the response.
        //   The AuthResponse DTO only has message, token, email, name fields.
    }

    // ================================================================
    // LOGIN
    // ================================================================

    /**
     * Authenticates a user and returns a JWT.
     *
     * HOW DOES THE PASSWORD COMPARISON WORK?
     * We use Spring Security's AuthenticationManager.
     * It:
     *   1. Loads the User from DB by email (via our UserDetailsService in SecurityConfig)
     *   2. Calls passwordEncoder.matches(rawPassword, storedHash)
     *   3. If they match → returns an Authentication object
     *   4. If they don't → throws BadCredentialsException
     *
     * WHY NOT JUST CALL .equals() ON THE PASSWORDS?
     * Because the stored password IS NOT the original password!
     * It's a BCrypt hash. You can't compare a plain password to a hash with .equals().
     * BCrypt.matches() does the reverse computation correctly.
     *
     * @param request validated DTO
     * @return AuthResponse with the JWT token
     */
    public AuthResponse login(LoginRequest request) {
        try {
            // Step 1: Attempt authentication (password verification happens here)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),    // "username" (we use email)
                            request.getPassword()  // plain-text password from the request
                    )
            );
            // ↑ UsernamePasswordAuthenticationToken is Spring Security's standard
            //   way to pass credentials to the AuthenticationManager.
            //
            //   Under the hood, authenticationManager:
            //     1. Calls our UserDetailsService: loadUserByUsername(email)
            //        → does SELECT * FROM users WHERE email = ?
            //     2. Calls passwordEncoder.matches(request.getPassword(), storedHash)
            //     3. If match → returns an authenticated Authentication object
            //     4. If no match → throws BadCredentialsException

            // Step 2: Get the authenticated user's email from the authentication result
            String email = authentication.getName();
            // ↑ .getName() returns the principal name — the email we used to authenticate.

            // Step 3: Load the user object to get the name for the response
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found after authentication"));
            // ↑ We already know the user exists (authentication succeeded),
            //   but we need the User object to get the name for the response.

            // Step 4: Generate a JWT token
            String token = jwtService.generateToken(email);
            // ↑ jwtService.generateToken(email) creates a signed JWT with:
            //   - sub: email
            //   - iat: now
            //   - exp: now + 30 minutes

            // Step 5: Return the token + user info
            return new AuthResponse(token, user.getEmail(), user.getName(), "Login successful");

        } catch (BadCredentialsException e) {
            // ↑ BadCredentialsException = wrong password OR email doesn't exist
            //
            // IMPORTANT: We return the SAME generic message for both cases.
            // If we said "email not found" vs "wrong password", attackers could
            // use that difference to enumerate which emails are registered.
            // This is called "security by obscurity" — intentional vagueness.
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
