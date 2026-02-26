package com.securevision.smarthome;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

// SmartHome integration tests moved to smarthome package
// so they can access package-private classes

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Smart Home Integration Tests")
public class SmartHomeTest {

    private static final Logger logger = LoggerFactory.getLogger(SmartHomeTest.class);

    @Test
    @Order(1)
    @DisplayName("Smart Home: Full OOP integration test - controller manages multiple devices")
    void testSmartHomeIntegration() {
        logger.info("Testing Smart Home Management System - integration");

        DeviceRepository repository = new DeviceRepository();
        SmartHomeNotificationService notificationService = new EmailNotificationService();
        SmartHomeController controller = new SmartHomeController(repository, notificationService, Clock.systemDefaultZone());

        SmartThermostat thermostat = new SmartThermostat("THERM-001", "Living Room Thermostat");
        SmartLight light = new SmartLight("LIGHT-001", "Kitchen Light", true);

        repository.addDevice(thermostat);
        repository.addDevice(light);

        assertThatCode(() -> controller.controlAllDevices("on"))
                .as("Controller should turn on all devices without error")
                .doesNotThrowAnyException();

        assertThat(thermostat.getStatus())
                .as("Thermostat should be ONLINE after turning on")
                .isEqualTo(DeviceStatus.ONLINE);

        assertThat(light.getStatus())
                .as("Light should be ONLINE after turning on")
                .isEqualTo(DeviceStatus.ONLINE);

        // Test temperature control with encapsulation validation
        assertThat(thermostat.setTargetTemperature(24.0))
                .as("Setting temperature within range (24.0) should succeed")
                .isTrue();

        assertThat(thermostat.setTargetTemperature(100.0))
                .as("Setting temperature out of range (100.0) should fail")
                .isFalse();

        // Test brightness control
        assertThat(light.setBrightness(75))
                .as("Setting brightness to 75 on dimmable light should succeed")
                .isTrue();

        logger.info("Smart Home integration test completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Automation: applyAutomationRules() should adjust devices based on time of day")
    void testAutomationRulesExecute() {
        DeviceRepository repository = new DeviceRepository();
        SmartHomeNotificationService notificationService = new EmailNotificationService();
        SmartHomeController controller = new SmartHomeController(repository, notificationService, Clock.systemDefaultZone());

        SmartThermostat thermostat = new SmartThermostat("THERM-AUTO", "Test Thermostat");
        SmartLight light = new SmartLight("LIGHT-AUTO", "Test Light", true);
        thermostat.turnOn();
        light.turnOn();
        repository.addDevice(thermostat);
        repository.addDevice(light);

        // Should run without throwing regardless of what time the test runs
        assertThatCode(() -> controller.applyAutomationRules())
                .as("Automation rules should execute without throwing any exception")
                .doesNotThrowAnyException();

        // Thermostat should have been set to either 21.0 or 18.0 depending on time
        // Both are valid within range so setTargetTemperature must have returned true
        logger.info("Automation rules applied successfully for {} devices",
                repository.getAllDevices().size());
    }

    @Test
    @Order(3)
    @DisplayName("Automation: Night mode dims lights to 20% when hour is 23:00")
    void testNightModeSetsBrightnessCorrectly() {
        DeviceRepository repository = new DeviceRepository();
        SmartHomeNotificationService notificationService = new EmailNotificationService();

        // This is the key — we fix the clock at 23:00 so the test always
        // behaves the same regardless of when it runs
        Clock fixedNightClock = Clock.fixed(
            Instant.parse("2024-01-01T23:00:00Z"),
            ZoneId.of("UTC")
        );

        SmartHomeController controller = new SmartHomeController(
            repository, notificationService, fixedNightClock
        );

        SmartLight light = new SmartLight("LIGHT-001", "Test Light", true);
        light.turnOn();
        repository.addDevice(light);

        controller.applyAutomationRules();

        // Now we can actually assert the specific brightness value
        // because we controlled what time the system thinks it is
        assertThat(light.getBrightness())
            .as("At 23:00, night mode should dim light to 20%")
            .isEqualTo(20);
    }
}
