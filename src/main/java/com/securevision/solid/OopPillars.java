package com.securevision.solid;

import java.util.List;

// Task 1 - Four pillars of OOP
// I used a Vehicle/Car hierarchy for abstraction and inheritance because
// it is a domain I can explain easily. BankAccount for encapsulation because
// it has an obvious real-world need to protect its balance from direct access.

// Abstraction - hide implementation details behind abstract methods

abstract class Vehicle {
    protected String make, model;
    protected int year;

    public Vehicle(String make, String model, int year) {
        this.make = make;
        this.model = model;
        this.year = year;
    }

    // Abstract methods - subclasses must implement
    public abstract void startEngine();
    public abstract void stopEngine();
    public abstract double calculateFuelEfficiency();

    // Concrete method available to all subclasses
    public String getVehicleInfo() {
        return year + " " + make + " " + model;
    }
}

// Encapsulation - private fields with validated public access

class BankAccount {
    private String accountNumber;
    private double balance;
    private String ownerName;

    public BankAccount(String accountNumber, String ownerName, double initialBalance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = initialBalance > 0 ? initialBalance : 0;
    }

    // Controlled access to balance
    public double getBalance() {
        return balance;
    }

    // Controlled modification with business rules
    public boolean withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    // Private helper method - implementation detail hidden from outside
    private boolean validateTransaction(double amount) {
        return amount > 0 && amount <= balance;
    }
}

// Inheritance - Dog reuses Animal behaviour and adds its own

class Animal {
    protected String name;
    protected int age;

    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public void eat() {
        System.out.println(name + " is eating");
    }

    public void sleep() {
        System.out.println(name + " is sleeping");
    }

    public String getName() { return name; }
    public int getAge() { return age; }
}

// Derived class inheriting from Animal
class Dog extends Animal {
    private String breed;

    public Dog(String name, int age, String breed) {
        super(name, age); // Call parent constructor
        this.breed = breed;
    }

    // Override parent method
    @Override
    public void eat() {
        System.out.println(name + " the dog is eating dog food");
    }

    // New method specific to Dog
    public void bark() {
        System.out.println(name + " is barking: Woof!");
    }

    public String getBreed() { return breed; }
}

// Polymorphism - same interface, different behaviour per class

interface Drawable {
    void draw();
    double calculateArea();
}

class DrawableRectangle implements Drawable {
    private double width, height;

    public DrawableRectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw() {
        System.out.println("Drawing a rectangle: " + width + "x" + height);
    }

    @Override
    public double calculateArea() {
        return width * height;
    }
}

class DrawableCircle implements Drawable {
    private double radius;

    public DrawableCircle(double radius) {
        this.radius = radius;
    }

    @Override
    public void draw() {
        System.out.println("Drawing a circle with radius: " + radius);
    }

    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}

// Polymorphism in action - same interface, different behaviour
class DrawingApplication {
    public static void processShapes(List<Drawable> shapes) {
        for (Drawable shape : shapes) {
            shape.draw(); // Polymorphic method call
            System.out.println("Area: " + shape.calculateArea());
        }
    }
}
