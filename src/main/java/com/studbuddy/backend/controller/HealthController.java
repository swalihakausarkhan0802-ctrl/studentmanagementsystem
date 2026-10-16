package com.studbuddy.backend.controller;
// ↑ This file is in the "controller" sub-package.
//   Physical location: src/main/java/com/studbuddy/backend/controller/

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
// ↑ HashMap and Map are built-in Java classes for key-value pairs
//   (like a JavaScript object: { "key": "value" })

/**
 * HealthController.java
 *
 * PURPOSE: A simple REST API to verify our backend is running.
 *
 * WHAT IS A CONTROLLER?
 * In the MVC (Model-View-Controller) pattern:
 *   - Model     → the data (Student object, etc.)
 *   - View      → what the user sees (in our case: JSON responses)
 *   - Controller → receives HTTP requests and returns responses
 *
 * A Controller is like a receptionist: it receives requests,
 * figures out what to do, and sends back a response.
 *
 * In React, you call fetch() or axios() → that request arrives here.
 *
 * DIFFERENCE: Controller vs Service
 *   - Controller: Handles HTTP requests/responses. Knows about URLs.
 *   - Service: Contains business logic. Doesn't know about HTTP.
 *   Example: Controller receives "GET /students", calls StudentService
 *            to get data, then returns JSON. Controller doesn't do math;
 *            it delegates to Service.
 *
 * For this simple health check, we don't need a separate Service.
 */

@RestController
// ↑ This annotation tells Spring: "This class is a controller that returns
//   data directly as the HTTP response body (usually JSON)."
//
//   @RestController = @Controller + @ResponseBody
//   - @Controller: This is a Spring-managed controller class
//   - @ResponseBody: Whatever you return from the method, send it as JSON in the HTTP response
//
//   Without @RestController, Spring would try to return an HTML page (view name),
//   not JSON data.

@RequestMapping("/api")
// ↑ All endpoints in this controller start with "/api"
//   So any @GetMapping inside here will be prefixed with "/api"
//   Example: @GetMapping("/health") becomes "/api/health"
//
//   This is a URL prefix/grouping — like having a base route in React Router

public class HealthController {
    // ↑ Java class definition.
    //   Java requires: [access modifier] class [ClassName] { ... }

    @GetMapping("/health")
    // ↑ This maps HTTP GET requests to the path "/api/health"
    //   (combined with @RequestMapping("/api") above)
    //
    //   Types of HTTP mappings:
    //   - @GetMapping    → GET request (read data)
    //   - @PostMapping   → POST request (create data)
    //   - @PutMapping    → PUT request (update data)
    //   - @DeleteMapping → DELETE request (delete data)
    //
    //   We only need GET for a health check because we're just READING status,
    //   not creating or modifying anything.

    public Map<String, String> healthCheck() {
        // ↑ "public" = anyone can call this method
        //   "Map<String, String>" = return type: a key-value map where both keys and values are Strings
        //                          (like a JavaScript object: { "status": "UP" })
        //   "healthCheck" = method name
        //   "()" = no parameters needed for a simple health check

        // HashMap is a data structure that stores key-value pairs
        // Like: const response = {} in JavaScript
        Map<String, String> response = new HashMap<>();
        // ↑ "Map<String, String>" is the type (interface)
        //   "new HashMap<>()" creates the actual object
        //   This is like: const response = new Map() in JavaScript

        response.put("status", "UP");
        // ↑ .put(key, value) adds a key-value pair
        //   Like: response.status = "UP" or response["status"] = "UP" in JavaScript

        response.put("application", "StudbuddY");
        // ↑ Adds: "application": "StudbuddY"

        return response;
        // ↑ Spring Boot + Jackson (JSON library) automatically converts this Map
        //   into JSON and sends it as the HTTP response body:
        //   {
        //     "status": "UP",
        //     "application": "StudbuddY"
        //   }
        //
        //   You don't have to write res.json() like in Express.js —
        //   @RestController handles that for you automatically!
    }
}
