package com.studbuddy.backend.repository;

import com.studbuddy.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository.java
 *
 * Same pattern as StudentRepository — extends JpaRepository,
 * Spring generates the implementation automatically.
 *
 * We only need one custom query method:
 *   findByEmail → used during login to look up the user
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    // ↑ Spring generates: SELECT * FROM users WHERE email = ?
    //   Returns Optional<User>:
    //     - User found  → Optional.of(user)
    //     - Not found   → Optional.empty()

    boolean existsByEmail(String email);
    // ↑ Used during registration to check for duplicate emails.
    //   SELECT COUNT(*) > 0 FROM users WHERE email = ?
}
