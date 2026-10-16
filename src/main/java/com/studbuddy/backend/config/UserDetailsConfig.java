package com.studbuddy.backend.config;

import com.studbuddy.backend.entity.User;
import com.studbuddy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

/**
 * UserDetailsConfig.java — Teaches Spring Security How to Load Users
 *
 * WHAT IS UserDetailsService?
 * It's a Spring Security interface with ONE method:
 *   UserDetails loadUserByUsername(String username)
 *
 * During login, the AuthenticationManager calls:
 *   userDetailsService.loadUserByUsername("aarav@example.com")
 * to find the user record, then compares the password.
 *
 * "username" in Spring Security terms = whatever you use to identify a user.
 * For us, that's an email address.
 *
 * WHY A SEPARATE CONFIG CLASS?
 * We could make our User entity implement UserDetails, but that mixes
 * JPA concerns with Spring Security concerns in one class.
 * Keeping them separate is cleaner — this small config class is the bridge.
 *
 * WHY NOT PUT THIS IN SecurityConfig?
 * SecurityConfig and this class both need to reference each other through beans.
 * Splitting them into two classes avoids circular dependency issues in Spring.
 */
@Configuration
public class UserDetailsConfig {

    private final UserRepository userRepository;

    @Autowired
    public UserDetailsConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns our custom UserDetailsService implementation.
     *
     * This lambda is essentially implementing the interface:
     *   interface UserDetailsService {
     *       UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
     *   }
     *
     * Spring Security calls this during authentication.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // ↑ "username" here is actually the email (we use email as identifier)
            // lambda syntax: username -> { body }
            // equivalent to: new UserDetailsService() { public UserDetails loadUserByUsername(...) {...} }

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with email: " + username
                    ));
            // ↑ If email doesn't exist, Spring Security throws UsernameNotFoundException.
            //   This gets translated to BadCredentialsException by the AuthenticationManager
            //   (Spring Security hides whether the email or password was wrong).

            // Spring Security's built-in User object (different from our entity User!)
            // This adapts our entity to what Spring Security expects.
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())         // identifier
                    .password(user.getPassword())          // BCrypt hash (NOT plain text)
                    .authorities(List.of(
                            new SimpleGrantedAuthority("ROLE_" + user.getRole())
                    ))                                     // e.g., "ROLE_USER"
                    .build();
            // ↑ Note: we use the FULL class name here because our entity is also called User.
            //   org.springframework.security.core.userdetails.User  ← Spring Security's User
            //   com.studbuddy.backend.entity.User                   ← our entity
        };
    }
}
