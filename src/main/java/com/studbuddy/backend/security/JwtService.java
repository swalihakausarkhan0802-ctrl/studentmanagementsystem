package com.studbuddy.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JwtService.java — JWT Creation and Validation
 *
 * This class has two main jobs:
 *   1. GENERATE a JWT after a successful login
 *   2. VALIDATE an incoming JWT and extract information from it
 *
 * WHAT IS A JWT?
 * JWT = JSON Web Token. Three Base64-encoded parts separated by dots:
 *
 *   HEADER.PAYLOAD.SIGNATURE
 *   eyJhbGciOiJIUzI1NiJ9  .  eyJzdWIiOiJhYXJhdiJ9  .  X7kJq3...
 *
 * Header:    { "alg": "HS256" }                  ← which algorithm
 * Payload:   { "sub": "user@email.com",           ← the claims
 *              "iat": 1728900000,                 ← issued at (Unix timestamp)
 *              "exp": 1728901800 }                ← expires at
 * Signature: HMAC-SHA256(header + "." + payload, secretKey)
 *            ← tamper-proof seal
 *
 * Anyone can decode the header and payload (they're just Base64).
 * But nobody can FORGE a valid signature without the secret key.
 * So the server trusts a token only if the signature matches.
 *
 * KEY CHOICES:
 * - Algorithm: HS256 (HMAC + SHA-256) — symmetric, fast, standard for simple APIs
 * - Secret: read from application.properties via @Value, never hardcoded
 * - Expiration: 30 minutes (configurable)
 *
 * @Component marks this as a Spring bean — it will be injected where needed.
 */
@Component
public class JwtService {

    @Value("${jwt.secret}")
    // ↑ @Value reads a value from application.properties and injects it.
    //   Spring replaces "${jwt.secret}" with the actual string from the properties file.
    //   Think of it like: process.env.JWT_SECRET in Node.js.
    private String secretString;

    @Value("${jwt.expiration}")
    private long expirationMs;
    // 1800000 = 30 minutes in milliseconds

    /**
     * Build the SecretKey object from the Base64-encoded string.
     * This is called each time we need to sign or verify a token.
     *
     * Keys.hmacShaKeyFor() verifies that the key is long enough for HS256
     * (at least 256 bits = 32 bytes). If it's too short, JJWT throws an exception.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretString);
        // ↑ Base64.getDecoder().decode() converts our Base64 string to raw bytes.
        //   The raw bytes are the actual secret key material.
        return Keys.hmacShaKeyFor(keyBytes);
        // ↑ Wraps the bytes into a SecretKey object that JJWT can use to sign/verify.
    }

    /**
     * GENERATE a JWT token for a given email (used as the "subject" claim).
     *
     * The "subject" (sub) is the standard JWT claim that identifies who this token is for.
     * We use the user's email — unique and non-sensitive enough for a claim.
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        // ↑ now = right now
        //   expiry = now + 30 minutes (or whatever expirationMs is)

        return Jwts.builder()
                // ↑ Jwts.builder() starts the fluent builder chain

                .subject(email)
                // ↑ Sets the "sub" (subject) claim to the user's email.
                //   This is how we know WHICH user this token belongs to.

                .issuedAt(now)
                // ↑ Sets "iat" — "issued at" timestamp. Used for audit trails.

                .expiration(expiry)
                // ↑ Sets "exp" — the token is INVALID after this timestamp.
                //   JJWT automatically rejects expired tokens during parsing.

                .signWith(getSigningKey())
                // ↑ Signs the token using our secret key + HS256 algorithm.
                //   Without the correct key, nobody can produce a valid signature.

                .compact();
                // ↑ Produces the final compact JWT string: "header.payload.signature"
    }

    /**
     * EXTRACT the email (subject) from a validated token.
     * Call this after validateToken() returns true.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
        // ↑ parseClaims() validates the signature and expiry, then returns the Claims object.
        //   .getSubject() reads the "sub" claim — our user's email.
    }

    /**
     * VALIDATE a token: check signature + check expiry.
     * Returns true if valid, false if anything is wrong.
     * Never throws an exception to callers — just returns false for bad tokens.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            // ↑ If this doesn't throw, the token is valid.
            return true;
        } catch (ExpiredJwtException e) {
            // Token was valid but has passed its expiry time
            // We log at debug level — not an error, just expired
            System.out.println("JWT expired: " + e.getMessage());
            return false;
        } catch (SignatureException e) {
            // Token's signature doesn't match our key — tampered or from wrong server
            System.out.println("Invalid JWT signature: " + e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            // Token is not structured like a valid JWT at all
            System.out.println("Malformed JWT: " + e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            // Token uses an algorithm or format we don't support
            System.out.println("Unsupported JWT: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            // Token string is null or empty
            System.out.println("JWT claims string is empty: " + e.getMessage());
            return false;
        }
    }

    /**
     * Parse the JWT and return the Claims (payload).
     * Throws exceptions if the token is invalid, expired, or tampered.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                // ↑ Starts the parser builder chain

                .verifyWith(getSigningKey())
                // ↑ Sets the key to verify the signature with.
                //   If the signature doesn't match, throws SignatureException.

                .build()
                // ↑ Builds the parser

                .parseSignedClaims(token)
                // ↑ Parses the token string.
                //   Also checks the "exp" claim — throws ExpiredJwtException if past expiry.

                .getPayload();
                // ↑ Returns the Claims object (the payload) if everything is valid.
    }
}
