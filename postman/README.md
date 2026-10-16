# StudbuddY API — Postman Testing Guide

## Quick Start

1. **Start the backend** (must be running before any test):
   ```bash
   cd StudbuddY/backend
   mvn spring-boot:run
   ```
   Look for: `Started BackendApplication in X seconds`

2. **Import the collection into Postman**:
   - Open Postman → **Import** button (top left)
   - Select file: `backend/postman/StudbuddY.postman_collection.json`
   - Click **Import**

3. **Verify the `baseUrl` variable**:
   - Open the collection → **Variables** tab
   - `baseUrl` should be `http://localhost:8080`
   - Change it only if you modified `server.port` in `application.properties`

4. **Run the Login request** to auto-populate `{{authToken}}`:
   - Go to `02 - Authentication → Login - Valid Credentials`
   - Click **Send**
   - The test script saves the JWT to `{{authToken}}` automatically

5. **Run any student request** — they all use `{{authToken}}` in the header.

---

## Base URL

```
http://localhost:8080
```

Configured in the collection variable `{{baseUrl}}`.

---

## Collection Structure

```
StudbuddY API Tests/
├── 01 - Health Check/
│   └── GET Health                          → 200 OK (no token needed)
│
├── 02 - Authentication/
│   ├── Register - Valid User               → 201 Created
│   ├── Register - Duplicate Email          → 409 Conflict
│   ├── Register - Missing Fields           → 400 Bad Request
│   ├── Login - Valid Credentials           → 200 OK + JWT token
│   ├── Login - Wrong Password              → 401 Unauthorized
│   └── Login - Unknown Email               → 401 Unauthorized
│
├── 03 - Student CRUD/
│   ├── POST Create Student                 → 201 Created
│   ├── GET All Students                    → 200 OK
│   ├── GET Student by ID                   → 200 OK
│   ├── PUT Update Student                  → 200 OK
│   ├── GET Student (Confirm Update)        → 200 OK
│   ├── DELETE Student                      → 204 No Content
│   └── GET Deleted Student (expect 404)    → 404 Not Found
│
└── 04 - Negative Tests/
    ├── GET Students - No Token             → 401 Unauthorized
    ├── GET Students - Malformed Token      → 401 Unauthorized
    ├── GET Students - Wrong Signature      → 401 Unauthorized
    ├── POST Create Student - Bad Payload   → 400 Bad Request
    ├── GET Student - Nonexistent ID        → 404 Not Found
    └── POST Create Student - Dup Email     → 409 Conflict
```

---

## Complete API Reference

### 1. Health Check

| Field | Value |
|-------|-------|
| Method | `GET` |
| URL | `{{baseUrl}}/api/health` |
| Auth | None required |
| Expected Status | `200 OK` |
| Response Body | `{ "status": "UP", "application": "StudbuddY" }` |

---

### 2. Registration

| Field | Value |
|-------|-------|
| Method | `POST` |
| URL | `{{baseUrl}}/api/auth/register` |
| Auth | None required |
| Content-Type | `application/json` |
| Expected Status | `201 Created` |

**Request Body:**
```json
{
  "name": "Your Name",
  "email": "you@example.com",
  "password": "YourPassword123!"
}
```

**Success Response (201):**
```json
{
  "message": "Registration successful. Please log in."
}
```

**Validation Rules:**
- `name` — required, not blank
- `email` — required, must be valid email format
- `password` — required, minimum 8 characters

**Common Errors:**
| Status | Cause | Response |
|--------|-------|----------|
| `400` | Blank fields or invalid email | `{ "errors": { "name": "...", "email": "..." } }` |
| `409` | Email already registered | `{ "message": "An account with email '...' already exists." }` |

> ⚠️ **Never** send plain passwords in screenshots or reports.

---

### 3. Login

| Field | Value |
|-------|-------|
| Method | `POST` |
| URL | `{{baseUrl}}/api/auth/login` |
| Auth | None required |
| Content-Type | `application/json` |
| Expected Status | `200 OK` |

**Request Body:**
```json
{
  "email": "you@example.com",
  "password": "YourPassword123!"
}
```

**Success Response (200):**
```json
{
  "token": "eyJhbGc...<redacted>",
  "email": "you@example.com",
  "name": "Your Name",
  "message": "Login successful"
}
```

**How the Postman test script saves the token automatically:**
```javascript
// Inside the Login request → Tests tab:
var json = pm.response.json();
pm.collectionVariables.set('authToken', json.token);
// Now {{authToken}} is available in all subsequent requests
```

**How to set the header manually (if needed):**
```
Authorization: Bearer eyJhbGc...<your token here>
```

**Common Errors:**
| Status | Cause |
|--------|-------|
| `401` | Wrong password OR unknown email (same message always — by design) |

---

### 4. Student CRUD — All Require JWT

Add this header to **every** student request:
```
Authorization: Bearer {{authToken}}
```

#### Create Student — `POST /api/students`

```json
{
  "name": "Aarav Sharma",
  "email": "aarav@example.com",
  "branch": "Computer Engineering",
  "year": 2
}
```

**Success (201):**
```json
{
  "id": 1,
  "name": "Aarav Sharma",
  "email": "aarav@example.com",
  "branch": "Computer Engineering",
  "year": 2
}
```

The test script saves `id` to `{{studentId}}` automatically.

**Validation Rules:**
- `name` — required, not blank, max 100 chars
- `email` — required, valid email, unique
- `branch` — required, not blank, max 100 chars
- `year` — required, between 1 and 4 (inclusive)

#### Get All Students — `GET /api/students`

Returns `200 OK` with a JSON array. Returns `[]` (empty array) if no students exist — NOT a 404.

#### Get by ID — `GET /api/students/{{studentId}}`

Returns `200 OK` with the student, or `404 Not Found` if the ID doesn't exist.

#### Update Student — `PUT /api/students/{{studentId}}`

Send all fields (same as create). Returns `200 OK` with updated student.

#### Delete Student — `DELETE /api/students/{{studentId}}`

Returns `204 No Content` with an **empty body**. The student is permanently removed from MySQL.

---

## CRUD Test Sequence (Run in This Order)

1. **Login** → saves token to `{{authToken}}`
2. **POST Create Student** → saves ID to `{{studentId}}`
3. **GET All Students** → confirm new student appears in array
4. **GET Student by ID** → confirm correct student returned
5. **PUT Update Student** → change name, branch, or year
6. **GET Student (Confirm Update)** → verify changes persisted
7. **DELETE Student** → 204 response
8. **GET Deleted Student** → verify 404 response

---

## Expected Status Codes

| Scenario | Status Code |
|----------|-------------|
| Successful data retrieval | `200 OK` |
| Successful resource creation | `201 Created` |
| Successful deletion | `204 No Content` |
| Invalid input (validation failure) | `400 Bad Request` |
| Missing or invalid JWT | `401 Unauthorized` |
| Resource not found | `404 Not Found` |
| Duplicate email or data conflict | `409 Conflict` |

---

## Security Test Cases

| Test | What to send | Expected Status |
|------|-------------|-----------------|
| No Authorization header | *(nothing)* | `401` |
| `Authorization: Bearer bad.jwt.here` | Malformed token | `401` |
| Valid JWT structure, wrong signature | Tampered token | `401` |
| Correct JWT, expired | Expired token | `401` |
| Correct JWT, valid | Real token from login | `200` / `201` / `204` |

---

## Why These Tests Matter (For Your Viva)

**Q: Why does a missing token return 401 and not 403?**
A: 401 Unauthorized = "I don't know who you are." 403 Forbidden = "I know who you are, but you're not allowed." Since there's no token, we don't know the identity → 401.

**Q: Why do we test with wrong passwords?**
A: To confirm that the server correctly rejects bad credentials and doesn't leak user data. The response should give the same message for both "wrong password" and "unknown email" — never hinting which one failed.

**Q: Why does GET /api/students return 200 with [] and not 404 when empty?**
A: The endpoint itself exists and worked. An empty list is not an error — it just means no records exist yet. 404 means the URL itself doesn't exist.

**Q: Why test token expiry?**
A: Tokens expire after 30 minutes (configured in `application.properties`). After expiry the server should return 401, not 200. This verifies that the security is truly time-limited.

---

## Screenshot Checklist for College Submission

Capture these screenshots in Postman (or browser console):

| # | Screenshot to Capture | Where |
|---|----------------------|-------|
| 1 | Backend startup — `Started BackendApplication in X seconds` | Terminal |
| 2 | `GET /api/health` → 200 response | Postman / Browser |
| 3 | `POST /api/auth/register` → 201 response | Postman |
| 4 | `POST /api/auth/login` → 200 with token (blur/redact the token) | Postman |
| 5 | `POST /api/students` → 201 with ID in response | Postman |
| 6 | `GET /api/students` → 200 with array | Postman |
| 7 | `GET /api/students/{id}` → 200 with one student | Postman |
| 8 | `PUT /api/students/{id}` → 200 with updated fields | Postman |
| 9 | `DELETE /api/students/{id}` → 204 No Content | Postman |
| 10 | `GET /api/students` with NO token → 401 | Postman |
| 11 | MySQL Workbench → `SELECT * FROM studbuddy.students` | MySQL Workbench |
| 12 | MySQL Workbench → `SELECT id, name, email, role FROM studbuddy.users` | MySQL Workbench |

> ⚠️ **Do NOT** take screenshots showing the full JWT token, password, or BCrypt hash.

---

## Persistence Verification

After creating a student, verify MySQL persistence:

```sql
-- Open MySQL Workbench and run:
USE studbuddy;

-- Check students
SELECT * FROM students;

-- Check users (password column shows BCrypt hash, never plain text)
SELECT id, name, email, role FROM users;
```

**Restart test** — stop the backend with `Ctrl+C`, then restart with `mvn spring-boot:run`.
After restart:
- `GET /api/health` → still 200 ✅
- Login with same credentials → still works ✅ (MySQL, not memory)
- Student list → same records still there ✅

This proves data is in **MySQL** (persistent), not RAM (would be lost on restart).

---

## Files in This Folder

| File | Purpose |
|------|---------|
| `StudbuddY.postman_collection.json` | Import into Postman — contains all 16 requests with test scripts |
| `README.md` | This guide |

---

## Importing the Collection

1. Open **Postman**
2. Click **Import** (top left)
3. Choose **File** tab
4. Select: `StudbuddY/backend/postman/StudbuddY.postman_collection.json`
5. Click **Import**
6. The collection `StudbuddY API Tests` appears in your sidebar

Then:
1. Expand `02 - Authentication`
2. Open `Login - Valid Credentials`
3. Click **Send**
4. Check the **Test Results** tab — "Token saved to collection variable" should pass
5. Now all `{{authToken}}` references work automatically
