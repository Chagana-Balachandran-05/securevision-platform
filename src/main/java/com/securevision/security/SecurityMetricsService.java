package com.securevision.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

// Security metrics service - tracks auth attempts and security events
// I added this class separately from SecureUserService to keep monitoring
// concerns isolated. If I wanted to swap from console logging to a proper
// monitoring dashboard like Prometheus, I only change this class.

public class SecurityMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(SecurityMetricsService.class);

    private final AtomicLong securityEventCount = new AtomicLong(0);
    private final AtomicLong authenticationCount = new AtomicLong(0);
    private long totalAuthDurationMs = 0;

    public SecurityMetricsService() {
        logger.info("SecurityMetricsService initialised");
    }

    public void recordSecurityEvent(String eventType, String severity) {
        securityEventCount.incrementAndGet();
        logger.info("[METRICS] Security event recorded - Type: {} | Severity: {}",
                eventType, severity);
    }

    public void recordAuthenticationLatency(Duration duration) {
        authenticationCount.incrementAndGet();
        totalAuthDurationMs += duration.toMillis();
        logger.debug("[METRICS] Authentication latency recorded: {}ms", duration.toMillis());
    }

    public long getTotalSecurityEvents() {
        return securityEventCount.get();
    }

    public long getTotalAuthentications() {
        return authenticationCount.get();
    }

    public double getAverageAuthLatencyMs() {
        long count = authenticationCount.get();
        return count > 0 ? (double) totalAuthDurationMs / count : 0.0;
    }

    public void printMetricsSummary() {
        logger.info("=== Security Metrics Summary ===");
        logger.info("Total Security Events: {}", getTotalSecurityEvents());
        logger.info("Total Authentications: {}", getTotalAuthentications());
        logger.info("Average Auth Latency: {}ms", String.format("%.2f", getAverageAuthLatencyMs()));
    }
}
