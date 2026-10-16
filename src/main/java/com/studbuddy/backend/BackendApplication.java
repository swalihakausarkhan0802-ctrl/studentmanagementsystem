package com.studbuddy.backend;
// ↑ "package" declares which folder/namespace this file belongs to.
//   Think of it like the folder path: com/studbuddy/backend/
//   Every Java file must declare its package.

// "import" brings in a class from another package — like "import" in JavaScript/React
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BackendApplication.java
 *
 * This is the ENTRY POINT of our entire Spring Boot application.
 * When you run the app, Java calls the main() method in this class first.
 *
 * In Java, everything lives inside a CLASS.
 * A class is like a blueprint or a container for related code.
 * Unlike JavaScript, you cannot write code outside a class in Java.
 */

@SpringBootApplication
// ↑ This single annotation does THREE things (it's actually 3 annotations in 1):
//   1. @Configuration      → This class can define Spring "beans" (objects Spring manages)
//   2. @EnableAutoConfiguration → Spring Boot automatically sets up things based on what
//                                  libraries are on the classpath (e.g., if MySQL driver is
//                                  present, it sets up a database connection automatically)
//   3. @ComponentScan      → Tells Spring to scan THIS package and all sub-packages
//                            for controllers, services, repositories, etc.
//
//   Without this, Spring Boot would not know about our HealthController.

public class BackendApplication {
    // ↑ "public" = accessible from anywhere
    //   "class" = we are defining a class (blueprint)
    //   "BackendApplication" = the name of this class (must match the filename)

    public static void main(String[] args) {
        // ↑ "public static void main(String[] args)" is the standard Java entry point.
        //   When you run any Java program, JVM (Java Virtual Machine) looks for this
        //   exact method signature and calls it first.
        //
        //   Comparison with JavaScript:
        //   - JavaScript: code runs top-to-bottom in a script file
        //   - Java: code must be inside a class, inside a main() method

        SpringApplication.run(BackendApplication.class, args);
        // ↑ This single line:
        //   1. Starts the Spring Boot framework
        //   2. Starts the embedded Tomcat web server on port 8080
        //   3. Scans for all controllers, services, etc.
        //   4. Connects to the database (once configured)
        //   5. Makes our app ready to receive HTTP requests
        //
        //   "BackendApplication.class" = pass the current class as a reference
        //   "args" = command-line arguments (usually empty during development)
    }
}
