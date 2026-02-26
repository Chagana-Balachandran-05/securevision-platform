package com.securevision.solid;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

// Task 4 - Tests for SOLID principles
// I used AssertJ instead of plain JUnit assertEquals because when a test
// fails the error message tells you exactly what went wrong in plain English.

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Task 1 - SOLID Principles and OOP Tests")
public class SolidPrinciplesTest {

    private static final Logger logger = LoggerFactory.getLogger(SolidPrinciplesTest.class);

    // SRP tests

    @Test
    @Order(1)
    @DisplayName("SRP: UserRepository should only handle saving users")
    void testUserRepositorySingleResponsibility() {
        logger.info("Testing SRP - UserRepository");

        UserRepository repo = new UserRepository();
        User user = new User("Alice", "alice@example.com");

        // UserRepository should only save - no email sending, no validation
        assertThatCode(() -> repo.saveUser(user))
                .as("UserRepository.saveUser() should execute without error")
                .doesNotThrowAnyException();
    }

    @Test
    @Order(2)
    @DisplayName("SRP: UserValidator should only validate user data")
    void testUserValidatorSingleResponsibility() {
        logger.info("Testing SRP - UserValidator");

        UserValidator validator = new UserValidator();

        User validUser = new User("Bob", "bob@example.com");
        User invalidUser = new User(null, null);

        assertThat(validator.validateUser(validUser))
                .as("Valid user should pass validation")
                .isTrue();

        assertThat(validator.validateUser(invalidUser))
                .as("User with null name and email should fail validation")
                .isFalse();
    }

    // OCP tests

    @Test
    @Order(3)
    @DisplayName("OCP: AreaCalculator should work with any Shape subclass")
    void testAreaCalculatorOpenClosed() {
        logger.info("Testing OCP - AreaCalculator");

        AreaCalculator calculator = new AreaCalculator();

        List<Shape> shapes = Arrays.asList(
                new Rectangle(5.0, 3.0),
                new Circle(4.0)
        );

        double totalArea = calculator.calculateTotalArea(shapes);

        double expectedRectArea = 5.0 * 3.0;
        double expectedCircleArea = Math.PI * 4.0 * 4.0;
        double expectedTotal = expectedRectArea + expectedCircleArea;

        assertThat(totalArea)
                .as("Total area should be sum of rectangle and circle areas")
                .isCloseTo(expectedTotal, within(0.001));
    }

    @Test
    @Order(4)
    @DisplayName("OCP: Rectangle area calculation is correct")
    void testRectangleArea() {
        Shape rectangle = new Rectangle(6.0, 4.0);

        assertThat(rectangle.calculateArea())
                .as("Rectangle 6x4 should have area 24.0")
                .isEqualTo(24.0);
    }

    @Test
    @Order(5)
    @DisplayName("OCP: Circle area calculation is correct")
    void testCircleArea() {
        Shape circle = new Circle(5.0);

        assertThat(circle.calculateArea())
                .as("Circle with radius 5 area should match PI * 25")
                .isCloseTo(Math.PI * 25, within(0.001));
    }

    // LSP tests

    @Test
    @Order(6)
    @DisplayName("LSP: Eagle should fly and eat as a Bird")
    void testEagleLSP() {
        logger.info("Testing LSP - Eagle extends Bird implements Flyable");

        Eagle eagle = new Eagle();

        assertThatCode(() -> eagle.eat())
                .as("Eagle should be able to eat (inherited from Bird)")
                .doesNotThrowAnyException();

        assertThatCode(() -> eagle.fly())
                .as("Eagle should be able to fly (implements Flyable)")
                .doesNotThrowAnyException();
    }

    @Test
    @Order(7)
    @DisplayName("LSP: PenguinLSP should swim and eat but not fly")
    void testPenguinLSP() {
        logger.info("Testing LSP - PenguinLSP extends Bird implements Swimmable");

        PenguinLSP penguin = new PenguinLSP();

        assertThatCode(() -> penguin.eat())
                .as("Penguin should eat (inherited from Bird)")
                .doesNotThrowAnyException();

        assertThatCode(() -> penguin.swim())
                .as("Penguin should swim (implements Swimmable)")
                .doesNotThrowAnyException();

        // Penguin is NOT Flyable - correct LSP design
        assertThat(penguin)
                .as("Penguin should NOT be an instance of Flyable")
                .isNotInstanceOf(Flyable.class);
    }

    // ISP tests

    @Test
    @Order(8)
    @DisplayName("ISP: Human implements all interfaces correctly")
    void testHumanISP() {
        logger.info("Testing ISP - Human implements Workable, Eatable, Sleepable");

        Human human = new Human();

        assertThat(human).isInstanceOf(Workable.class);
        assertThat(human).isInstanceOf(Eatable.class);
        assertThat(human).isInstanceOf(Sleepable.class);

        assertThatCode(() -> {
            human.work();
            human.eat();
            human.sleep();
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(9)
    @DisplayName("ISP: Robot only implements Workable - not forced to implement unneeded methods")
    void testRobotISP() {
        logger.info("Testing ISP - Robot implements only Workable");

        Robot robot = new Robot();

        assertThat(robot).isInstanceOf(Workable.class);
        assertThat(robot).isNotInstanceOf(Eatable.class);
        assertThat(robot).isNotInstanceOf(Sleepable.class);

        assertThatCode(() -> robot.work())
                .doesNotThrowAnyException();
    }

    // DIP tests

    @Test
    @Order(10)
    @DisplayName("DIP: NotificationManager works with EmailNotification")
    void testNotificationManagerWithEmail() {
        logger.info("Testing DIP - NotificationManager depends on abstraction");

        NotificationService emailService = new EmailNotification();
        NotificationManager manager = new NotificationManager(emailService);

        assertThatCode(() -> manager.sendNotification("Test message", "user@example.com"))
                .as("NotificationManager should send via email without error")
                .doesNotThrowAnyException();
    }

    @Test
    @Order(11)
    @DisplayName("DIP: NotificationManager works with SMSNotification without code change")
    void testNotificationManagerWithSMS() {
        logger.info("Testing DIP - swapping implementation without changing NotificationManager");

        // Same high-level module, different low-level implementation
        NotificationService smsService = new SMSNotification();
        NotificationManager manager = new NotificationManager(smsService);

        assertThatCode(() -> manager.sendNotification("Test SMS", "0771234567"))
                .as("NotificationManager should send via SMS without error")
                .doesNotThrowAnyException();
    }

    @Test
    @Order(12)
    @DisplayName("Abstraction: Vehicle subclass must implement all abstract methods")
    void testAbstractionVehicle() {
        Vehicle car = new Vehicle("Toyota", "Corolla", 2023) {
            @Override public void startEngine() { System.out.println("Engine started"); }
            @Override public void stopEngine()  { System.out.println("Engine stopped");  }
            @Override public double calculateFuelEfficiency() { return 15.5; }
        };
        assertThat(car.getVehicleInfo()).isEqualTo("2023 Toyota Corolla");
        assertThat(car.calculateFuelEfficiency()).isEqualTo(15.5);
    }

    @Test
    @Order(13)
    @DisplayName("Encapsulation: BankAccount enforces business rules — overdraft rejected")
    void testEncapsulationBankAccount() {
        BankAccount account = new BankAccount("ACC-001", "Alice", 500.0);
        assertThat(account.getBalance()).isEqualTo(500.0);
        assertThat(account.withdraw(200.0)).isTrue();
        assertThat(account.getBalance()).isEqualTo(300.0);
        assertThat(account.withdraw(999.0)).isFalse();
        account.deposit(100.0);
        assertThat(account.getBalance()).isEqualTo(400.0);
    }

    @Test
    @Order(14)
    @DisplayName("Inheritance: Dog inherits from Animal and overrides eat()")
    void testInheritanceDogAnimal() {
        Dog dog = new Dog("Rex", 5, "Labrador");
        assertThat(dog.getName()).isEqualTo("Rex");
        assertThat(dog.getBreed()).isEqualTo("Labrador");
        assertThatCode(() -> dog.eat()).doesNotThrowAnyException();
        assertThatCode(() -> dog.bark()).doesNotThrowAnyException();
    }

    @Test
    @Order(15)
    @DisplayName("Polymorphism: DrawingApplication processes shapes via Drawable interface")
    void testPolymorphismDrawingApplication() {
        DrawableRectangle rect   = new DrawableRectangle(4.0, 5.0);
        DrawableCircle    circle = new DrawableCircle(3.0);
        assertThat(rect.calculateArea()).isEqualTo(20.0);
        assertThat(circle.calculateArea()).isCloseTo(Math.PI * 9, within(0.001));
        List<Drawable> shapes = Arrays.asList(rect, circle);
        assertThatCode(() -> DrawingApplication.processShapes(shapes))
                .doesNotThrowAnyException();
    }
}
