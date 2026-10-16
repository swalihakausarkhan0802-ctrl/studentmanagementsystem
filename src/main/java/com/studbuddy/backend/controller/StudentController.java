package com.studbuddy.backend.controller;

import com.studbuddy.backend.entity.Student;
import com.studbuddy.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StudentController.java — The HTTP Layer
 *
 * WHAT DOES THIS CLASS DO?
 * 1. Listens for HTTP requests at specific URLs.
 * 2. Extracts data from the request (URL path, JSON body).
 * 3. Delegates the actual work to StudentService.
 * 4. Returns an appropriate HTTP response (status code + JSON body).
 *
 * WHAT DOES IT NOT DO?
 * It does NOT contain business logic.
 * It does NOT talk directly to the database.
 * It does NOT decide whether a duplicate email is okay.
 * Those decisions belong in StudentService.
 *
 * SECURITY NOTE:
 * These endpoints have no authentication right now.
 * Anyone on localhost can call them.
 * This is intentional for Phase 3 (learning CRUD).
 * Phase 4 will add JWT authentication to protect these endpoints.
 *
 * HOW DO URLs MAP TO METHODS?
 * @RequestMapping("/api/students") → all endpoints start with /api/students
 *
 * Then each method adds its own path:
 *   POST   /api/students           → createStudent()
 *   GET    /api/students           → getAllStudents()
 *   GET    /api/students/{id}      → getStudentById()
 *   PUT    /api/students/{id}      → updateStudent()
 *   DELETE /api/students/{id}      → deleteStudent()
 *
 * {id} is a PATH VARIABLE — the actual number in the URL.
 * Example: GET /api/students/3 → id = 3
 */

@RestController
// ↑ Marks this as a REST controller.
//   Returns JSON directly (no HTML pages).
//   = @Controller + @ResponseBody

@RequestMapping("/api/students")
// ↑ All endpoints in this controller start with "/api/students".

public class StudentController {

    private final StudentService studentService;

    @Autowired
    // ↑ Spring injects the StudentService bean here.
    //   You don't create it with "new StudentService()" — Spring manages it.
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // ================================================================
    // CREATE — POST /api/students
    // Expected response: 201 Created
    // ================================================================
    @PostMapping
    // ↑ Maps HTTP POST requests to /api/students (no extra path needed)

    public ResponseEntity<Student> createStudent(@Valid @RequestBody Student student) {
        // ↑ @RequestBody → tells Spring to read the HTTP request body (JSON)
        //                   and convert it into a Student Java object.
        //                   Jackson does this conversion automatically.
        //                   { "name": "Aarav", ... } → Student object with name="Aarav", ...
        //
        //   @Valid        → triggers Bean Validation on the Student object.
        //                   Spring checks all the @NotBlank, @Email, @Min, @Max annotations.
        //                   If any constraint fails, Spring throws MethodArgumentNotValidException
        //                   BEFORE this method even runs.
        //                   Our GlobalExceptionHandler catches that and returns 400.

        Student created = studentService.createStudent(student);
        // ↑ Delegate to service. The service handles duplicate checks and saves the student.

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
        // ↑ Return HTTP 201 Created + the saved Student as JSON in the response body.
        //   The saved student includes the auto-generated id from MySQL.
        //   HTTP 201 is the standard for "resource successfully created."
    }

    // ================================================================
    // READ ALL — GET /api/students
    // Expected response: 200 OK
    // ================================================================
    @GetMapping
    // ↑ Maps HTTP GET requests to /api/students

    public ResponseEntity<List<Student>> getAllStudents() {
        List<Student> students = studentService.getAllStudents();
        return ResponseEntity.ok(students);
        // ↑ ResponseEntity.ok(body) is a shortcut for:
        //   ResponseEntity.status(HttpStatus.OK).body(body)
        //   Returns HTTP 200 + the list of students as a JSON array.
        //   If no students exist: returns 200 with empty array [] (NOT a 404).
    }

    // ================================================================
    // READ ONE — GET /api/students/{id}
    // Expected response: 200 OK or 404 Not Found
    // ================================================================
    @GetMapping("/{id}")
    // ↑ {id} is a URL template variable (path variable).
    //   GET /api/students/1   → id = 1
    //   GET /api/students/42  → id = 42

    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        // ↑ @PathVariable extracts the {id} value from the URL and binds it
        //   to the "id" parameter. Spring converts the String "1" to Long 1 automatically.

        Student student = studentService.getStudentById(id);
        // ↑ If the student doesn't exist, the service throws StudentNotFoundException.
        //   GlobalExceptionHandler catches it and returns 404 — this line never returns null.

        return ResponseEntity.ok(student);
    }

    // ================================================================
    // UPDATE — PUT /api/students/{id}
    // Expected response: 200 OK or 404 Not Found or 409 Conflict
    // ================================================================
    @PutMapping("/{id}")
    // ↑ Maps HTTP PUT requests to /api/students/{id}

    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody Student student) {
        // ↑ Both @PathVariable and @RequestBody are used together here.
        //   @PathVariable Long id   → get the id from the URL (/api/students/3 → id=3)
        //   @RequestBody Student    → get the updated fields from the JSON request body

        Student updated = studentService.updateStudent(id, student);
        return ResponseEntity.ok(updated);
        // ↑ Returns HTTP 200 + the fully updated student as JSON.
    }

    // ================================================================
    // DELETE — DELETE /api/students/{id}
    // Expected response: 204 No Content or 404 Not Found
    // ================================================================
    @DeleteMapping("/{id}")
    // ↑ Maps HTTP DELETE requests to /api/students/{id}

    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        // ↑ ResponseEntity<Void> means the response has no body.
        //   HTTP 204 No Content is the standard for "successfully deleted, nothing to return."

        studentService.deleteStudent(id);
        // ↑ If student doesn't exist, service throws StudentNotFoundException → 404.
        //   If it does exist, student is deleted.

        return ResponseEntity.noContent().build();
        // ↑ Returns HTTP 204 No Content with an empty body.
        //   .build() creates the ResponseEntity with no body.
    }
}
