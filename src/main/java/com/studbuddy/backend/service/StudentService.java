package com.studbuddy.backend.service;

import com.studbuddy.backend.entity.Student;
import com.studbuddy.backend.exception.StudentNotFoundException;
import com.studbuddy.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * StudentService.java — The Business Logic Layer
 *
 * WHY A SERVICE LAYER?
 * The controller's job is ONLY to handle HTTP (read request, return response).
 * Business logic — "what are the rules for creating a student?" — belongs here.
 *
 * Benefits of separating logic into a service:
 *   1. You can test the business rules without starting the web server.
 *   2. Multiple controllers could reuse the same service.
 *   3. If you change your database, only the repository changes — the service stays.
 *   4. Clear separation makes code easier to read and debug.
 *
 * SHOULD WE USE AN INTERFACE + IMPLEMENTATION?
 * For large enterprise projects: yes, you'd create StudentService (interface)
 * and StudentServiceImpl (implementation).
 * For this assignment: one concrete class is simpler and just as correct.
 * We'll use a single class — no interface — to keep things beginner-friendly.
 *
 * WHAT IS @Service?
 * It marks this class as a Spring-managed "bean" (object).
 * Spring creates ONE instance of StudentService when the app starts
 * and makes it available to anyone who needs it (like StudentController).
 *
 * WHAT IS DEPENDENCY INJECTION?
 * Instead of writing: StudentRepository repo = new StudentRepository();
 * Spring automatically "injects" (provides) the StudentRepository object for us.
 * We just declare: @Autowired StudentRepository studentRepository;
 * Spring finds the existing repository bean and gives it to us.
 * This is "Inversion of Control" (IoC) — Spring controls object creation, not us.
 */

@Service
// ↑ Tells Spring: "Create one instance of this class and manage it."
//   Spring will inject this into StudentController using @Autowired.

public class StudentService {

    private final StudentRepository studentRepository;
    // ↑ "final" means we set this once (in the constructor) and never change it.
    //   This is constructor injection — the recommended way to inject dependencies.

    @Autowired
    // ↑ DEPENDENCY INJECTION in action.
    //   When Spring creates StudentService, it sees the constructor needs a
    //   StudentRepository. Spring already has one (from @Repository), so it
    //   automatically passes it here. You never call "new StudentRepository()".
    //
    //   Think of it like: "Spring, I need a StudentRepository — you provide it."
    //   Spring: "I've got one ready. Here you go."
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // ================================================================
    // CREATE
    // ================================================================

    /**
     * Creates a new student.
     *
     * Before saving, we check if the email already exists.
     * If it does, we throw an exception — the controller will return 409 Conflict.
     * The @Column(unique=true) in the entity is a database-level backup constraint.
     * This service-level check catches duplicates early with a friendly error.
     */
    public Student createStudent(Student student) {
        // Step 1: Check for duplicate email (application-level check)
        if (studentRepository.existsByEmail(student.getEmail())) {
            throw new IllegalArgumentException(
                    "A student with email '" + student.getEmail() + "' already exists."
            );
            // ↑ IllegalArgumentException signals "bad input".
            //   Our GlobalExceptionHandler will catch this and return 409 Conflict.
        }

        // Step 2: Save the student — Hibernate runs INSERT INTO students (...)
        return studentRepository.save(student);
        // ↑ .save() does two things:
        //   - If student.id == null → INSERT (create new row)
        //   - If student.id != null → UPDATE (update existing row)
        //   Since we receive a student with no id set, this is always an INSERT here.
        //   The returned Student object has the generated id filled in by MySQL.
    }

    // ================================================================
    // READ ALL
    // ================================================================

    /**
     * Returns all students from the database.
     * Hibernate runs: SELECT * FROM students
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
        // ↑ Returns a List<Student> — a Java list (like a JavaScript array) of Student objects.
        //   Jackson converts this to a JSON array automatically.
        //   If no students exist, returns an empty list []. Not null, not an error.
    }

    // ================================================================
    // READ ONE
    // ================================================================

    /**
     * Finds a student by their ID.
     * If no student has that ID, throws StudentNotFoundException (→ 404).
     */
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                // ↑ findById returns Optional<Student>.
                //   It may contain a student, or it may be empty.

                .orElseThrow(() -> new StudentNotFoundException(id));
                // ↑ .orElseThrow(supplier):
                //   "Give me the Student if present.
                //    If not present, THROW this exception instead."
                //
                //   () -> new StudentNotFoundException(id) is a LAMBDA EXPRESSION.
                //   A lambda is an inline, anonymous function — like an arrow function in JS:
                //   JavaScript: () => new Error("not found")
                //   Java:       () -> new StudentNotFoundException(id)
    }

    // ================================================================
    // UPDATE
    // ================================================================

    /**
     * Updates an existing student.
     *
     * Important rules:
     * 1. The student must exist (otherwise → 404, not a silent create).
     * 2. If the email is being changed, the new email must not belong to another student.
     */
    public Student updateStudent(Long id, Student updatedStudent) {
        // Step 1: Verify the student exists (throws 404 if not)
        Student existing = getStudentById(id);
        // ↑ We reuse our own getStudentById method.
        //   If this throws StudentNotFoundException, updateStudent also throws it.
        //   The controller catches it and returns 404.

        // Step 2: Check if the email is changing AND the new email belongs to a different student
        if (!existing.getEmail().equals(updatedStudent.getEmail())) {
            // ↑ The email is being changed (new email != old email)
            if (studentRepository.existsByEmail(updatedStudent.getEmail())) {
                // New email is already used by someone else → conflict
                throw new IllegalArgumentException(
                        "A student with email '" + updatedStudent.getEmail() + "' already exists."
                );
            }
        }

        // Step 3: Apply the updates to the existing student object
        existing.setName(updatedStudent.getName());
        existing.setEmail(updatedStudent.getEmail());
        existing.setBranch(updatedStudent.getBranch());
        existing.setYear(updatedStudent.getYear());
        // ↑ We update the fields on the EXISTING object (which already has the correct id).
        //   We do NOT use updatedStudent.id because the client shouldn't control the id.

        // Step 4: Save — Hibernate runs UPDATE students SET name=?, email=?, ... WHERE id=?
        return studentRepository.save(existing);
        // ↑ Since existing.id is not null, .save() runs an UPDATE, not an INSERT.
    }

    // ================================================================
    // DELETE
    // ================================================================

    /**
     * Deletes a student by ID.
     * If the student doesn't exist, returns 404 rather than silently doing nothing.
     */
    public void deleteStudent(Long id) {
        // Step 1: Verify the student exists before deleting
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException(id);
            // ↑ existsById(id) → SELECT COUNT(*) ... WHERE id = ?
            //   If count is 0, student doesn't exist → throw 404.
        }

        // Step 2: Delete — Hibernate runs: DELETE FROM students WHERE id = ?
        studentRepository.deleteById(id);
    }
}
