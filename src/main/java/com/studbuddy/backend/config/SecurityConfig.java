package com.studbuddy.backend.config;

import com.studbuddy.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * SecurityConfig.java — The Spring Security Control Panel
 *
 * WHAT DOES THIS CLASS DO?
 * It defines HOW Spring Security behaves for our application.
 * Without this class, Spring Security locks down EVERYTHING by default
 * (including the health endpoint) and shows a login form.
 * With this class, we control exactly:
 *   - Which routes are public (no token needed)
 *   - Which routes require authentication (valid JWT required)
 *   - That sessions are stateless (JWT replaces sessions)
 *   - Where our JWT filter sits in the chain
 *
 * KEY ANNOTATIONS:
 *   @Configuration  → this class defines Spring beans (like @Bean methods)
 *   @EnableWebSecurity → activates Spring Security's web support
 *
 * @Bean METHODS:
 * Methods annotated with @Bean tell Spring: "create one instance of whatever
 * this method returns and make it available for injection everywhere."
 * For example: passwordEncoder() returns a BCryptPasswordEncoder.
 * Any class that needs a PasswordEncoder gets this exact instance injected.
 *
 * CSRF EXPLANATION:
 * CSRF (Cross-Site Request Forgery) attacks trick a logged-in browser user into
 * making unwanted requests. Traditional session-based APIs need CSRF tokens.
 * REST APIs that use JWT in the Authorization header (not cookies) are NOT
 * vulnerable to CSRF because:
 *   - The browser cannot automatically send the Authorization header from another site
 *   - Only your JavaScript code (or Postman) can set it explicitly
 * Therefore: disabling CSRF for a stateless JWT API is CORRECT and standard practice.
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * The MAIN security configuration — defines the SecurityFilterChain.
     *
     * WHAT IS A SecurityFilterChain?
     * Every HTTP request to your app passes through a series of filters.
     * The SecurityFilterChain is the ordered list of those filters plus the rules:
     *   "which URLs are public, which require authentication?"
     *
     * This method returns the fully configured chain as a Spring bean.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ---- CORS ----
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // ↑ Enable CORS using our corsConfigurationSource() bean.
            //   This allows the Vite frontend (running on localhost:3000 or 5173)
            //   to make requests to this Spring Boot backend (localhost:8080).
            //   Without this, the browser blocks every request with a CORS error.

            // ---- CSRF ----
            .csrf(csrf -> csrf.disable())
            // ↑ Disable CSRF protection. Safe for a stateless JWT REST API.
            //   (Explained above in the class Javadoc)

            // ---- SESSION MANAGEMENT ----
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // ↑ STATELESS = Spring Security will NOT create or use HTTP sessions.
            //   Every request must carry its own JWT — the server "forgets" the user
            //   between requests. This is the correct mode for JWT authentication.

            // ---- AUTHORIZATION RULES ----
            .authorizeHttpRequests(auth -> auth

                // PUBLIC endpoints — no token needed
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                // ↑ .permitAll() = anyone can call these, with or without a token
                //   These are the only unprotected routes.

                // PROTECTED endpoints — valid JWT required
                .requestMatchers("/api/students/**").authenticated()
                // ↑ .authenticated() = the request must have a valid JWT.
                //   If no token or invalid token → 401 Unauthorized
                //   If valid token but wrong role → 403 Forbidden (not used here)

                // Everything else also requires authentication (safe default)
                .anyRequest().authenticated()
            )

            // ---- JWT FILTER POSITION ----
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            // ↑ Insert our JWT filter BEFORE Spring's default username/password filter.
            //   Order matters: our filter must run first so it can populate the
            //   SecurityContext before the authorization check happens.

            // ---- EXCEPTION HANDLING ----
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    // ↑ AuthenticationEntryPoint is called when an UNAUTHENTICATED request
                    //   reaches a protected endpoint (no token, or invalid/expired token).
                    //
                    // WHY IS THIS NEEDED?
                    // Without this, Spring Security routes unauthenticated requests through
                    // AccessDeniedException, which returns 403 Forbidden.
                    // That is WRONG — 403 means "authenticated but not allowed."
                    // 401 means "not authenticated" — this is the correct response
                    // when no valid JWT is provided.
                    //
                    // This lambda directly writes a 401 response with a JSON body.
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"status\":401,\"error\":\"Unauthorized\"," +
                        "\"message\":\"Authentication required. Please provide a valid Bearer token.\"}" 
                    );
                })
            );

        return http.build();
        // ↑ .build() finalizes and returns the configured SecurityFilterChain.
    }

    /**
     * BCrypt password encoder.
     *
     * WHAT IS BCrypt?
     * BCrypt is a hashing function designed for passwords:
     *   - SLOW by design — takes ~100ms — makes brute-force attacks impractical
     *   - Includes a random "salt" — same password hashes differently each time
     *   - Cost factor (default 10) — controls how slow it is (higher = more secure)
     *
     * Example:
     *   encoder.encode("ExamplePass123!")
     *   → "$2a$10$X7kJq3BrqtM8Tj..." (always 60 chars, always different)
     *
     *   encoder.matches("ExamplePass123!", "$2a$10$X7kJq3...")
     *   → true (BCrypt can verify without decrypting)
     *
     * This bean is injected into AuthService for registration and login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager — Spring Security's authentication coordinator.
     *
     * AuthService uses this to verify login credentials.
     * We ask Spring for the existing AuthenticationManager rather than creating one,
     * because Spring Boot has already wired it up based on our UserDetailsService.
     * We expose it as a @Bean so AuthService can @Autowired inject it.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * CORS Configuration
     *
     * Allows the Vite frontend development server to call our backend.
     *
     * Allowed origins:
     *   - http://localhost:3000  (Vite dev server — StudbuddY frontend uses port 3000)
     *   - http://localhost:5173  (Vite default port — fallback)
     *
     * Allowed methods: the HTTP verbs our StudentController uses.
     * Allowed headers: Content-Type and Authorization (for the JWT).
     * Allow credentials: false (we use Authorization header, not cookies).
     *
     * In production, replace the allowed origins with your actual deployed frontend URL.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Only allow requests from the actual frontend origins
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // StudbuddY Vite dev server
            "http://localhost:5173"   // Vite default port (fallback)
        ));

        // Allow the HTTP methods used by StudentController and AuthController
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        // OPTIONS is needed for CORS preflight requests sent by the browser

        // Allow the headers that our frontend sends
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",   // Required: contains "Bearer <JWT>"
            "Content-Type",    // Required: tells server we're sending JSON
            "Accept"           // Standard header
        ));

        // We use Authorization header (not cookies) so credentials = false
        configuration.setAllowCredentials(false);

        // How long the browser can cache the preflight response (in seconds)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        // ↑ Apply these CORS rules only to our API paths
        return source;
    }
}
