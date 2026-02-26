package com.securevision.solid;

import java.util.List;

// Task 1 - SOLID Principles
// I started with SRP because it is the easiest to get wrong without noticing.
// My original design had one UserManager doing everything — splitting it out
// made the classes much easier to test individually.

// SRP - single responsibility

// VIOLATING SRP (Bad example from document)
class UserManagerBad {
    public void saveUser(User user) { /* database logic */ }
    public void sendEmail(User user, String message) { /* email logic */ }
    public boolean validateUser(User user) { return true; /* validation logic */ }
}

// FOLLOWING SRP - Each class has one responsibility
class UserRepository {
    public void saveUser(User user) {
        // Database persistence logic
        System.out.println("Saving user: " + user.getName());
    }
}

class EmailService {
    public void sendEmail(User user, String message) {
        // Email sending logic
        System.out.println("Sending email to: " + user.getEmail());
    }
}

class UserValidator {
    public boolean validateUser(User user) {
        // Validation logic
        return user.getName() != null && user.getEmail() != null;
    }
}

// Simple User class to support examples above
class User {
    private String name;
    private String email;

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
}

// OCP - open for extension, closed for modification
abstract class Shape {
    public abstract double calculateArea();
}

class Rectangle extends Shape {
    private double width, height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double calculateArea() {
        return width * height;
    }
}

class Circle extends Shape {
    private double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}

// Area calculator - closed for modification but open for extension
class AreaCalculator {
    public double calculateTotalArea(List<Shape> shapes) {
        return shapes.stream()
                .mapToDouble(Shape::calculateArea)
                .sum();
    }
}

// LSP - substitutability of subtypes

class Bird {
    public void eat() {
        System.out.println("Bird is eating");
    }
}

class FlyingBird extends Bird {
    public void fly() {
        System.out.println("Bird is flying");
    }
}

class Penguin extends Bird {
    public void swim() {
        System.out.println("Penguin is swimming");
    }
}

// Proper LSP implementation using interfaces
interface Flyable {
    void fly();
}

interface Swimmable {
    void swim();
}

class Eagle extends Bird implements Flyable {
    @Override
    public void fly() {
        System.out.println("Eagle soars high");
    }
}

class PenguinLSP extends Bird implements Swimmable {
    @Override
    public void swim() {
        System.out.println("Penguin swims gracefully");
    }
}

// LSP in practice — feedBird() accepts any Bird subtype without breaking
class BirdFeederDemo {
    public static void feedBird(Bird bird) {
        bird.eat(); // works for Eagle, PenguinLSP, or any future Bird subtype
    }
    // feedBird(new Eagle())      → Bird is eating   ✓ LSP satisfied
    // feedBird(new PenguinLSP()) → Bird is eating   ✓ LSP satisfied
    // Both subtypes substitute correctly — no program break
}

// ISP - no client should depend on methods it doesn't use

// VIOLATING ISP - Fat interface
interface WorkerBad {
    void work();
    void eat();
    void sleep();
}

// FOLLOWING ISP - Segregated interfaces
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

interface Sleepable {
    void sleep();
}

// Human worker implementing all interfaces
class Human implements Workable, Eatable, Sleepable {
    @Override
    public void work() { System.out.println("Human working"); }

    @Override
    public void eat() { System.out.println("Human eating"); }

    @Override
    public void sleep() { System.out.println("Human sleeping"); }
}

// Robot worker implementing only relevant interface
class Robot implements Workable {
    @Override
    public void work() { System.out.println("Robot working efficiently"); }
}

// DIP - depend on abstractions not concretions

interface NotificationService {
    void send(String message, String recipient);
}

// Low-level modules
class EmailNotification implements NotificationService {
    @Override
    public void send(String message, String recipient) {
        System.out.println("Email sent to: " + recipient + " - " + message);
    }
}

class SMSNotification implements NotificationService {
    @Override
    public void send(String message, String recipient) {
        System.out.println("SMS sent to: " + recipient + " - " + message);
    }
}

// High-level module depending on abstraction (NOT on concrete class)
class NotificationManager {
    private NotificationService notificationService;

    public NotificationManager(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void sendNotification(String message, String recipient) {
        notificationService.send(message, recipient);
    }
}

// ─── How SOLID applies to the actual SmartHome system I built ───

// SRP in my system:
// SmartHomeController only controls devices.
// DeviceRepository only stores devices.
// SmartHomeNotificationService only sends alerts.
// I started with one "SmartHomeManager" class that did all three things —
// splitting it out made testing much easier because I could test each part alone.

// OCP in my system:
// If I need to add a SmartCamera device I only need to create a new class
// that extends SmartDevice. I do not touch SmartHomeController at all —
// it calls turnOn() and turnOff() via the abstract type and the new device
// works immediately. That is the open/closed principle working in practice.

// LSP in my system:
// SmartThermostat and SmartLight both extend SmartDevice.
// SmartHomeController treats them identically via controlAllDevices().
// Neither breaks the SmartDevice contract — they both implement
// turnOn(), turnOff(), getStatus(), and getDeviceType() correctly.

// ISP in my system:
// SmartThermostat only implements TemperatureControllable.
// SmartLight only implements Dimmable.
// Neither is forced to implement the other's interface.
// A thermostat has no concept of brightness, so it should never
// be required to implement setBrightness(). ISP prevents that.

// DIP in my system:
// SmartHomeController depends on SmartHomeNotificationService (interface).
// Not on EmailNotificationService (concrete class).
// I can swap email for SMS or push notification by creating a new class
// that implements the interface — the controller code does not change at all.
