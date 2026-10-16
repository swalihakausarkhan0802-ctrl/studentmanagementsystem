package com.studbuddy.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * BackendApplicationTests.java
 *
 * This is a test file. In Java/Spring Boot, tests are kept in:
 *   src/test/java/  (separate from main code in src/main/java/)
 *
 * @SpringBootTest: Loads the full Spring application context for testing.
 * @Test: Marks a method as a test that JUnit should run.
 *
 * "contextLoads" verifies that the Spring application context
 * starts without errors — a basic sanity check.
 *
 * We'll add more tests in later phases.
 */
@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // This test passes if the application context loads successfully.
        // If your pom.xml, application.properties, or any Spring bean
        // has a configuration error, this test will fail and tell you.
    }
}
