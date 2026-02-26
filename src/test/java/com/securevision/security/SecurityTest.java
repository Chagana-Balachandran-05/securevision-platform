package com.securevision.security;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 4 - Automated Security Test Suite
 *
 * This test class implements automated security testing across three layers,
 * corresponding to the SCA, SAST, and DAST categories defined in the
 * DevSecOps pipeline:
 *
 * LAYER 1 - Application Security Tests (SAST equivalent):
 * Tests in this class verify security logic at the application level —
 * SQL injection prevention, input validation, and authentication failure
 * handling. These mirror what SAST tools check but are executable as
 * automated JUnit tests that run in under 1 second.
 *
 * LAYER 2 - Dependency Vulnerability Scanning (SCA):
 * The OWASP Dependency Check plugin in pom.xml provides SCA scanning.
 * The test testOwaspDependencyCheckConfigured() in this class verifies
 * that the SCA tooling is correctly configured so it cannot be
 * accidentally removed.
 *
 * LAYER 3 - Dynamic Application Security Testing (DAST):
 * The GitHub Actions pipeline Stage 5 runs OWASP ZAP against a deployed
 * instance. The pipeline YAML configuration is verified by
 * testDastPipelineConfigured().
 *
 * Together these three layers implement the Shift Left principle described
 * in Task 2 — security is verified at every stage, not just at deployment.
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Task 2 - Security Testing (SCA, SAST, Input Validation)")
public class SecurityTest {

    private static final Logger logger = LoggerFactory.getLogger(SecurityTest.class);

    private SecureUserService secureUserService;
    private InMemoryUserRepository userRepository;
    private SimplePasswordEncoder passwordEncoder;
    private ConsoleAuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        passwordEncoder = new SimplePasswordEncoder();
        auditLogger = new ConsoleAuditLogger();
        userRepository = new InMemoryUserRepository();

        // Add a test user
        String encodedPassword = passwordEncoder.encode("securePass123");
        userRepository.addUser(new AuthUser("alice", encodedPassword, "alice@example.com"));

        secureUserService = new SecureUserService(passwordEncoder, auditLogger, userRepository);
    }

    // auth tests

    @Test
    @Order(1)
    @DisplayName("AUTH: Valid credentials should authenticate successfully")
    void testSuccessfulAuthentication() {
        logger.info("Testing successful authentication");

        AuthenticationResult result = secureUserService.authenticateUser("alice", "securePass123");

        assertThat(result)
                .as("Valid credentials should return SUCCESS")
                .isEqualTo(AuthenticationResult.SUCCESS);
    }

    @Test
    @Order(2)
    @DisplayName("AUTH: Wrong password should fail authentication")
    void testFailedAuthentication() {
        logger.info("Testing failed authentication with wrong password");

        AuthenticationResult result = secureUserService.authenticateUser("alice", "wrongpassword");

        assertThat(result)
                .as("Wrong password should return AUTHENTICATION_FAILED")
                .isEqualTo(AuthenticationResult.AUTHENTICATION_FAILED);
    }

    @Test
    @Order(3)
    @DisplayName("AUTH: Non-existent user should fail authentication")
    void testNonExistentUser() {
        logger.info("Testing authentication for non-existent user");

        AuthenticationResult result = secureUserService.authenticateUser("unknown", "anyPassword123");

        assertThat(result)
                .as("Non-existent user should return AUTHENTICATION_FAILED")
                .isEqualTo(AuthenticationResult.AUTHENTICATION_FAILED);
    }

    // input validation tests

    @Test
    @Order(4)
    @DisplayName("SAST: Null username should be rejected as INVALID_INPUT")
    void testNullUsernameRejected() {
        logger.info("Testing null username rejection");

        AuthenticationResult result = secureUserService.authenticateUser(null, "validPass123");

        assertThat(result)
                .as("Null username should be rejected")
                .isEqualTo(AuthenticationResult.INVALID_INPUT);
    }

    @Test
    @Order(5)
    @DisplayName("SAST: Short password (less than 8 chars) should be rejected")
    void testShortPasswordRejected() {
        logger.info("Testing short password rejection");

        AuthenticationResult result = secureUserService.authenticateUser("alice", "short");

        assertThat(result)
                .as("Password shorter than 8 characters should be rejected")
                .isEqualTo(AuthenticationResult.INVALID_INPUT);
    }

    @Test
    @Order(6)
    @DisplayName("SAST: SQL injection in username should be blocked")
    void testSqlInjectionBlocked() {
        logger.info("Testing SQL injection prevention");

        // Classic SQL injection payload
        String sqlInjection = "'; DROP TABLE users; --";
        AuthenticationResult result = secureUserService.authenticateUser(sqlInjection, "password123");

        assertThat(result)
                .as("SQL injection attempt in username should be rejected as INVALID_INPUT")
                .isEqualTo(AuthenticationResult.INVALID_INPUT);
    }

    @Test
    @Order(7)
    @DisplayName("SAST: SELECT SQL keyword in username should be blocked")
    void testSelectInjectionBlocked() {
        logger.info("Testing SELECT injection prevention");

        AuthenticationResult result = secureUserService.authenticateUser(
                "SELECT * FROM users", "password123"
        );

        assertThat(result)
                .as("SELECT statement in username should be rejected")
                .isEqualTo(AuthenticationResult.INVALID_INPUT);
    }

    @Test
    @Order(8)
    @DisplayName("SAST: Empty username should be rejected")
    void testEmptyUsernameRejected() {
        logger.info("Testing empty username rejection");

        AuthenticationResult result = secureUserService.authenticateUser("   ", "password123");

        assertThat(result)
                .as("Whitespace-only username should be rejected as INVALID_INPUT")
                .isEqualTo(AuthenticationResult.INVALID_INPUT);
    }

    // password encoder tests

    @Test
    @Order(9)
    @DisplayName("SAST: Password encoder should correctly verify matching passwords")
    void testPasswordEncoderMatches() {
        logger.info("Testing password encoder match");

        String rawPassword = "MySecurePassword123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(passwordEncoder.matches(rawPassword, encoded))
                .as("Correct password should match its encoded version")
                .isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("SAST: Password encoder should reject wrong password")
    void testPasswordEncoderRejectsWrong() {
        logger.info("Testing password encoder rejection of wrong password");

        String rawPassword = "MySecurePassword123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(passwordEncoder.matches("WrongPassword", encoded))
                .as("Wrong password should NOT match encoded version")
                .isFalse();
    }

    // metrics tests

    @Test
    @Order(11)
    @DisplayName("Metrics: SecurityMetricsService should track security events")
    void testSecurityMetricsTracking() {
        logger.info("Testing security metrics service");

        SecurityMetricsService metricsService = new SecurityMetricsService();

        metricsService.recordSecurityEvent("LOGIN_ATTEMPT", "INFO");
        metricsService.recordSecurityEvent("LOGIN_FAILURE", "WARNING");
        metricsService.recordSecurityEvent("BRUTE_FORCE_DETECTED", "CRITICAL");

        assertThat(metricsService.getTotalSecurityEvents())
                .as("Should have recorded 3 security events")
                .isEqualTo(3);
    }

    @Test
    @Order(12)
    @DisplayName("Metrics: Authentication latency should be tracked correctly")
    void testAuthenticationLatencyTracking() {
        logger.info("Testing authentication latency tracking");

        SecurityMetricsService metricsService = new SecurityMetricsService();

        metricsService.recordAuthenticationLatency(java.time.Duration.ofMillis(150));
        metricsService.recordAuthenticationLatency(java.time.Duration.ofMillis(250));

        assertThat(metricsService.getTotalAuthentications())
                .as("Should have recorded 2 authentication events")
                .isEqualTo(2);

        assertThat(metricsService.getAverageAuthLatencyMs())
                .as("Average latency should be 200ms")
                .isEqualTo(200.0);
    }

    // SCA and DAST configuration verification tests

    @Test
    @Order(13)
    @DisplayName("SCA: OWASP Dependency Check must be configured in pom.xml")
    void testOwaspDependencyCheckConfigured() throws Exception {
        logger.info("Verifying SCA layer configuration");

        // This test verifies the SCA layer cannot be accidentally removed
        // from the build configuration
        File pomFile = new File("pom.xml");
        assertThat(pomFile).as("pom.xml must exist in project root").exists();

        String pomContent = java.nio.file.Files.readString(pomFile.toPath());
        assertThat(pomContent)
            .as("SCA: OWASP dependency-check-maven plugin must be configured")
            .contains("dependency-check-maven");
        assertThat(pomContent)
            .as("SCA: failBuildOnCVSS threshold must be set")
            .contains("failBuildOnCVSS");
    }

    @Test
    @Order(14)
    @DisplayName("DAST: ZAP pipeline stage must be configured in CI/CD workflow")
    void testDastPipelineConfigured() throws Exception {
        logger.info("Verifying DAST layer configuration");

        // This test verifies the DAST layer is present in the pipeline
        File pipelineFile = new File(".github/workflows/devsecops-pipeline.yml");
        assertThat(pipelineFile)
            .as("DAST: GitHub Actions pipeline file must exist").exists();

        String pipelineContent = java.nio.file.Files.readString(pipelineFile.toPath());
        assertThat(pipelineContent)
            .as("DAST: ZAP baseline scan must be configured in pipeline")
            .contains("zap");
    }
}
