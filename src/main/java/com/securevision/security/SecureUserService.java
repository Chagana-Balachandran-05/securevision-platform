package com.securevision.security;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Task 2 - Secure authentication service
// Before writing this I read through the OWASP Top 10 (2021 edition).
// The two most relevant risks for this service are A03 (Injection) and
// A07 (Identification and Authentication Failures), so those are the
// two things I focused the security controls on.

// Authentication result enum
enum AuthenticationResult {
    SUCCESS,
    AUTHENTICATION_FAILED,
    RATE_LIMITED,
    INVALID_INPUT
}

// Simple User model for authentication
class AuthUser {
    private String username;
    private String passwordHash;
    private String email;

    public AuthUser(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
}

// Password encoder interface (abstraction - DIP applied)
interface PasswordEncoder {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}

// Simple password encoder implementation using SHA-256
class SimplePasswordEncoder implements PasswordEncoder {

    // I chose SHA-256 here because BCrypt requires a third-party dependency.
    // In production I would use BCrypt with work factor 12, but SHA-256 from
    // java.security is available in every JDK and is sufficient to demonstrate
    // the concept of one-way hashing for this task.
    @Override
    public String encode(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available on this JVM", e);
        }
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}

// Audit logger interface (abstraction - DIP applied)
interface AuditLogger {
    void logSecurityEvent(String event, String username);
}

// Console audit logger implementation
class ConsoleAuditLogger implements AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleAuditLogger.class);

    @Override
    public void logSecurityEvent(String event, String username) {
        logger.info("[SECURITY AUDIT] Event: {} | User: {} | Time: {}",
                event, username, System.currentTimeMillis());
    }
}

// Simple in-memory user repository
interface UserRepository {
    AuthUser findByUsername(String username);
}

class InMemoryUserRepository implements UserRepository {
    private final java.util.Map<String, AuthUser> users = new java.util.HashMap<>();

    public void addUser(AuthUser user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public AuthUser findByUsername(String username) {
        return users.get(username);
    }
}

// Main authentication service - brings together rate limiting, validation, and audit logging
public class SecureUserService {
    private static final Logger logger = LoggerFactory.getLogger(SecureUserService.class);

    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;
    private final RateLimiter rateLimiter;
    private final UserRepository userRepository;

    public SecureUserService(PasswordEncoder encoder,
                              AuditLogger logger,
                              UserRepository userRepository) {
        this.passwordEncoder = encoder;
        this.auditLogger = logger;
        this.rateLimiter = RateLimiter.create(10.0); // 10 attempts per second
        this.userRepository = userRepository;
    }

    public AuthenticationResult authenticateUser(String username, String password) {

        // Rate limiting - prevent brute force attacks
        if (!rateLimiter.tryAcquire()) {
            auditLogger.logSecurityEvent("Rate limit exceeded", username);
            return AuthenticationResult.RATE_LIMITED;
        }

        // Input validation
        if (!isValidInput(username, password)) {
            auditLogger.logSecurityEvent("Invalid input attempt", username);
            return AuthenticationResult.INVALID_INPUT;
        }

        // Secure password comparison
        AuthUser user = userRepository.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
            auditLogger.logSecurityEvent("Successful authentication", username);
            return AuthenticationResult.SUCCESS;
        }

        auditLogger.logSecurityEvent("Failed authentication", username);
        return AuthenticationResult.AUTHENTICATION_FAILED;
    }

    private boolean isValidInput(String username, String password) {
        // Security-first validation
        return username != null && !username.trim().isEmpty() &&
               password != null && password.length() >= 8 &&
               !containsSqlInjectionPatterns(username);
    }

    private boolean containsSqlInjectionPatterns(String input) {
        if (input == null) return false;
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("'") ||
               lowerInput.contains("--") ||
               lowerInput.contains("drop") ||
               lowerInput.contains("select") ||
               lowerInput.contains("insert") ||
               lowerInput.contains("delete") ||
               lowerInput.contains("union");
    }
}

/*
 * Security design decisions for SecureUserService.
 *
 * Why SHA-256 instead of BCrypt:
 * BCrypt requires a third-party library (Spring Security or jBCrypt).
 * SHA-256 is in the Java standard library and demonstrates the concept
 * of one-way hashing without adding a dependency. In a real system I
 * would use BCrypt with a work factor of 12 because it is adaptive —
 * as hardware gets faster you increase the work factor to keep brute
 * force attacks slow. SHA-256 alone does not have this property.
 *
 * Why Guava RateLimiter instead of a custom implementation:
 * I first tried implementing rate limiting with a HashMap of timestamps.
 * It worked in single-threaded tests but had a race condition when I
 * thought about concurrent requests. Guava's RateLimiter implements the
 * token bucket algorithm and is thread-safe, which saved me from writing
 * a concurrent data structure from scratch.
 *
 * Limitation - rate limiting is per service instance, not per user:
 * The current rate limiter allows 10 attempts per second total across all
 * users. A determined attacker could make 9 attempts per second per account
 * and never be blocked. A production system would track attempts per IP
 * address using Redis with a sliding window counter.
 *
 * Limitation - in-memory user repository:
 * InMemoryUserRepository is fine for testing but loses all data when the
 * application restarts. A real system would use a database with prepared
 * statements (not string concatenation) to prevent SQL injection at the
 * persistence layer.
 */
