package com.studbuddy.backend.security;

import com.studbuddy.backend.entity.User;
import com.studbuddy.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthenticationFilter.java — The JWT "Gatekeeper"
 *
 * WHAT IS A SERVLET FILTER?
 * In Java web applications, every HTTP request passes through a chain of "filters"
 * BEFORE reaching your controller. Think of it like airport security:
 *
 *   Request → [Filter 1] → [Filter 2] → [JwtFilter] → [Your Controller]
 *                                              ↑
 *                              We insert our filter here
 *
 * HOW DOES IT WORK FOR JWT?
 * For every incoming request:
 *   1. Read the "Authorization" header
 *   2. If it starts with "Bearer ", extract the token
 *   3. Validate the token (signature + expiry)
 *   4. Extract the email from the token
 *   5. Load the user from the database
 *   6. Tell Spring Security: "this request is from user X with role USER"
 *   7. Let the request continue to the controller
 *
 * If no token or invalid token:
 *   - Do NOT block the request here
 *   - Just don't set the authentication context
 *   - Spring Security's other filters will enforce "must be authenticated" for protected routes
 *
 * WHY OncePerRequestFilter?
 * In some frameworks, filters can run multiple times per request.
 * OncePerRequestFilter guarantees our filter runs EXACTLY ONCE per request.
 * This is the correct base class for a JWT filter in Spring Boot 3.x.
 */

@Component
// ↑ Spring manages this filter as a bean. SecurityConfig will register it.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain   // ← the rest of the filter chain
    ) throws ServletException, IOException {

        // ============================================================
        // STEP 1: Read the Authorization header
        // ============================================================
        final String authHeader = request.getHeader("Authorization");
        // ↑ The client sends: "Authorization: Bearer eyJhbGc..."
        //   If the header is missing, authHeader is null.

        // ============================================================
        // STEP 2: Check if it's a Bearer token
        // ============================================================
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token present OR wrong format.
            // Don't set authentication — just continue the filter chain.
            // If this endpoint requires authentication, Spring Security will
            // reject the request AFTER all filters run (returns 401).
            filterChain.doFilter(request, response);
            return;
            // ↑ "return" after doFilter is important — we don't want to
            //   continue with the code below for requests with no token.
        }

        // ============================================================
        // STEP 3: Extract the token string (remove "Bearer " prefix)
        // ============================================================
        final String token = authHeader.substring(7);
        // ↑ "Bearer " is 7 characters. substring(7) gives us just the token.
        //   "Bearer eyJhbGc..." → "eyJhbGc..."

        // ============================================================
        // STEP 4: Validate the token
        // ============================================================
        if (!jwtService.validateToken(token)) {
            // Token is expired, malformed, or has wrong signature.
            // Don't authenticate. Let the filter chain continue.
            // Spring Security will return 401 for protected routes.
            filterChain.doFilter(request, response);
            return;
        }

        // ============================================================
        // STEP 5: Extract user email from the token
        // ============================================================
        final String email = jwtService.extractEmail(token);

        // ============================================================
        // STEP 6: Check if security context is already set
        // (avoid doing DB lookup if already authenticated)
        // ============================================================
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // ↑ SecurityContextHolder.getContext().getAuthentication() == null
            //   means: "this request hasn't been authenticated yet in this filter chain"
            //   This avoids re-authenticating on every filter if multiple filters run.

            // ============================================================
            // STEP 7: Load the user from the database
            // ============================================================
            User user = userRepository.findByEmail(email).orElse(null);
            // ↑ We verify the user still exists in the DB.
            //   A token for a deleted user would be invalid even if the signature is correct.

            if (user != null) {
                // ============================================================
                // STEP 8: Build a Spring Security Authentication object
                // ============================================================
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),    // principal: who this is
                                null,               // credentials: null (we already validated the token)
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                                // ↑ authorities: what roles/permissions this user has
                                //   "ROLE_USER" → Spring Security understands "USER" role
                        );
                // ↑ UsernamePasswordAuthenticationToken is Spring Security's standard
                //   way to represent "an authenticated user."

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                // ↑ Attaches request metadata (IP address, session ID) to the token.
                //   Useful for audit logging.

                // ============================================================
                // STEP 9: Store the authentication in Spring's SecurityContext
                // ============================================================
                SecurityContextHolder.getContext().setAuthentication(authToken);
                // ↑ This is the KEY step.
                //   SecurityContextHolder is like a thread-local storage for auth info.
                //   After setting this, Spring Security considers this request
                //   "authenticated" for the rest of its lifecycle.
                //   All subsequent filters and the controller can call:
                //     SecurityContextHolder.getContext().getAuthentication()
                //   and get back the user's info.
            }
        }

        // ============================================================
        // STEP 10: Continue the filter chain
        // ============================================================
        filterChain.doFilter(request, response);
        // ↑ Pass control to the next filter, then eventually to the controller.
        //   If the controller endpoint is protected and we didn't authenticate above,
        //   Spring Security's authorization filter will reject it with 401.
    }
}
