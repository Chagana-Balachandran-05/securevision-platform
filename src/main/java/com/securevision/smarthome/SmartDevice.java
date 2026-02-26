package com.securevision.smarthome;

import java.time.Clock;
import java.time.LocalTime;
import java.util.*;

// Smart Home Management System - Task 1 complex application
// I chose a smart home because it naturally contains multiple device types
// with different capabilities, which made it a good fit for demonstrating
// polymorphism and interface segregation in a realistic context.

// Supporting enums
enum DeviceStatus { ONLINE, OFFLINE, ERROR, MAINTENANCE }
enum ThermostatMode { HEAT, COOL, AUTO, OFF }
enum AlertLevel { INFO, WARNING, ERROR, CRITICAL }

// Supporting interfaces
interface TemperatureControllable {
    boolean setTargetTemperature(double temperature);
}

interface Dimmable {
    boolean setBrightness(int level);
}

// Abstract base - all smart devices share state and core behaviour
public abstract class SmartDevice {
    protected String deviceId;
    protected String deviceName;
    protected boolean isOnline;
    protected DeviceStatus status;

    public SmartDevice(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.isOnline = false;
        this.status = DeviceStatus.OFFLINE;
    }

    // Abstract methods enforcing implementation in subclasses
    public abstract boolean turnOn();
    public abstract boolean turnOff();
    public abstract DeviceStatus getStatus();
    public abstract String getDeviceType();

    // Every subclass MUST implement this, which means the controller
    // never needs to know what type of device it is dealing with (OCP)
    public abstract void applyAutomationRule(int currentHour,
                                              SmartHomeNotificationService notificationService);

    // Concrete methods available to all devices
    public String getDeviceName() { return deviceName; }

    public String getDeviceInfo() {
        return String.format("Device: %s [%s] - Status: %s",
                deviceName, deviceId, status);
    }

    protected void setOnlineStatus(boolean online) {
        this.isOnline = online;
        this.status = online ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;
    }
}

// SmartThermostat - temperature bounds enforced through private fields
class SmartThermostat extends SmartDevice implements TemperatureControllable {
    private double currentTemperature;
    private double targetTemperature;
    private ThermostatMode mode;
    private final double MIN_TEMP = 10.0;
    private final double MAX_TEMP = 35.0;

    public SmartThermostat(String deviceId, String deviceName) {
        super(deviceId, deviceName);
        this.currentTemperature = 20.0;
        this.targetTemperature = 22.0;
        this.mode = ThermostatMode.AUTO;
    }

    // Encapsulated temperature control with validation
    @Override
    public boolean setTargetTemperature(double temperature) {
        if (temperature >= MIN_TEMP && temperature <= MAX_TEMP) {
            this.targetTemperature = temperature;
            adjustTemperature();
            return true;
        }
        return false;
    }

    @Override
    public boolean turnOn() {
        setOnlineStatus(true);
        return true;
    }

    @Override
    public boolean turnOff() {
        setOnlineStatus(false);
        this.mode = ThermostatMode.OFF;
        return true;
    }

    @Override
    public DeviceStatus getStatus() {
        return status;
    }

    @Override
    public String getDeviceType() {
        return "Smart Thermostat";
    }

    // Private method - implementation detail hidden
    private void adjustTemperature() {
        if (currentTemperature < targetTemperature) {
            this.mode = ThermostatMode.HEAT;
            System.out.println(deviceName + ": switching to HEAT mode " +
                "(target: " + targetTemperature + "°C, current: " + currentTemperature + "°C)");
        } else if (currentTemperature > targetTemperature) {
            this.mode = ThermostatMode.COOL;
            System.out.println(deviceName + ": switching to COOL mode " +
                "(target: " + targetTemperature + "°C, current: " + currentTemperature + "°C)");
        } else {
            this.mode = ThermostatMode.AUTO;
            System.out.println(deviceName + ": temperature at target, switching to AUTO");
        }
    }

    @Override
    public void applyAutomationRule(int currentHour,
                                     SmartHomeNotificationService notificationService) {
        // Thermostat handles its own comfort temperature rule
        // Daytime (7am to 11pm): set to comfortable 21 degrees
        // Night time: set to eco-friendly 18 degrees
        if (currentHour >= 7 && currentHour < 23) {
            setTargetTemperature(21.0);
        } else {
            setTargetTemperature(18.0);
        }
    }
}

// SmartLight extends SmartDevice and adds Dimmable capability
class SmartLight extends SmartDevice implements Dimmable {
    private int brightness;
    private String color;
    private boolean isDimmable;

    public SmartLight(String deviceId, String deviceName, boolean isDimmable) {
        super(deviceId, deviceName);
        this.brightness = 100;
        this.color = "white";
        this.isDimmable = isDimmable;
    }

    @Override
    public boolean turnOn() {
        setOnlineStatus(true);
        return true;
    }

    @Override
    public boolean turnOff() {
        setOnlineStatus(false);
        this.brightness = 0;
        return true;
    }

    @Override
    public DeviceStatus getStatus() {
        return status;
    }

    @Override
    public String getDeviceType() {
        return "Smart Light";
    }

    // Interface implementation
    @Override
    public boolean setBrightness(int level) {
        if (isDimmable && level >= 0 && level <= 100) {
            this.brightness = level;
            return true;
        }
        return false;
    }

    public int getBrightness() {
        return brightness;
    }

    @Override
    public void applyAutomationRule(int currentHour,
                                     SmartHomeNotificationService notificationService) {
        // Light handles its own night-mode dimming rule
        // Between 10pm and 6am, dim to 20% to save energy
        if (currentHour >= 22 || currentHour < 6) {
            setBrightness(20);
            notificationService.sendAlert(
                "Night mode: " + deviceName + " dimmed to 20%",
                AlertLevel.INFO
            );
        } else {
            setBrightness(100);
        }
    }
}

// DeviceRepository - only responsible for storing and retrieving devices
class DeviceRepository {
    private Map<String, SmartDevice> devices = new HashMap<>();

    public void addDevice(SmartDevice device) {
        devices.put(device.deviceId, device);
    }

    public SmartDevice findDevice(String deviceId) {
        return devices.get(deviceId);
    }

    public List<SmartDevice> getAllDevices() {
        return new ArrayList<>(devices.values());
    }
}

// Notification uses an interface so the controller never depends on email directly
interface SmartHomeNotificationService {
    void sendAlert(String message, AlertLevel level);
}

class EmailNotificationService implements SmartHomeNotificationService {
    @Override
    public void sendAlert(String message, AlertLevel level) {
        System.out.println("Email Alert [" + level + "]: " + message);
    }
}

// SmartHomeController - treats all devices uniformly via SmartDevice type
class SmartHomeController {
    private final DeviceRepository deviceRepository;
    private final SmartHomeNotificationService notificationService;
    private final Clock clock;

    // Dependency injection following DIP - Clock injected for testability
    public SmartHomeController(DeviceRepository repository,
                                SmartHomeNotificationService notificationService,
                                Clock clock) {
        this.deviceRepository = repository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    // Polymorphism: Handle different device types uniformly
    public void controlAllDevices(String command) {
        List<SmartDevice> devices = deviceRepository.getAllDevices();

        for (SmartDevice device : devices) {
            switch (command.toLowerCase()) {
                case "on":
                    if (device.turnOn()) {
                        notificationService.sendAlert(
                                device.getDeviceInfo() + " turned on",
                                AlertLevel.INFO
                        );
                    }
                    break;
                case "off":
                    if (device.turnOff()) {
                        notificationService.sendAlert(
                                device.getDeviceInfo() + " turned off",
                                AlertLevel.INFO
                        );
                    }
                    break;
            }
        }
    }

    /**
     * Applies time-based automation rules to all registered devices.
     * I added this after realising the controller was purely manual —
     * a real smart home needs to respond to conditions automatically,
     * not just wait for someone to type "on" or "off".
     */
    public void applyAutomationRules() {
        int currentHour = LocalTime.now(clock).getHour();
        List<SmartDevice> devices = deviceRepository.getAllDevices();

        // OCP in action: the controller does not know or care what type
        // each device is. Every device handles its own rule.
        // Adding a new device type (SmartCamera, SmartDoorLock) requires
        // ZERO changes to this method.
        for (SmartDevice device : devices) {
            device.applyAutomationRule(currentHour, notificationService);
        }
    }
}

/*
 * Design decisions and trade-offs for the Smart Home System.
 *
 * Why abstract class SmartDevice instead of an interface:
 * I initially considered making SmartDevice an interface but devices share
 * real state (deviceId, deviceName, status, isOnline) and shared behaviour
 * (setOnlineStatus, getDeviceInfo). An interface cannot hold state, so an
 * abstract class was the correct choice here. If SmartDevice had no shared
 * state I would have used an interface.
 *
 * Why separate Dimmable and TemperatureControllable interfaces:
 * Following ISP, a thermostat should never be forced to implement
 * setBrightness() just because a light has that capability.
 * Keeping them as separate opt-in interfaces means each device only
 * implements what it genuinely supports.
 *
 * Limitation - single-threaded design:
 * The current DeviceRepository uses a plain HashMap. In a real deployment
 * multiple devices could update their status concurrently, which would
 * cause race conditions. A production version would use ConcurrentHashMap
 * and an event queue for the automation rules.
 *
 * Limitation - automation rules use system clock directly:
 * applyAutomationRules() calls LocalTime.now() which makes it hard to test
 * in isolation. A better design would inject a Clock object so tests can
 * control what time the system thinks it is.
 */
