# SecureVision Platform

**Learner:** Chagana Balachandran
**Registration ID:** 49842-ALID
**Qualification:** ATHE Level 5 Extended Diploma in Computing

---

## About This Project

This project implements the four tasks from the Advanced Programming assignment. The code is organised as a single Maven project with packages separating each area of work.

---

## Project Structure

```
src/main/java/com/securevision/
│
├── solid/               Task 1 - SOLID principles and OOP pillars
│   ├── SolidPrinciples.java    All 5 SOLID principles (SRP, OCP, LSP, ISP, DIP)
│   └── OopPillars.java         Four OOP pillars (Abstraction, Encapsulation, Inheritance, Polymorphism)
│
├── smarthome/           Task 1 - Complex OOP application
│   └── SmartDevice.java        Smart Home Management System
│
├── security/            Task 2 - DevSecOps security-first design
│   ├── SecureUserService.java   Rate limiting, input validation, SQL injection prevention
│   └── SecurityMetricsService.java  Security event tracking
│
└── perception/          Task 3 - Large dataset application (ONCE dataset)
    ├── AutonomousPerceptionSystem.java   Main perception pipeline
    └── DataProcessingUtils.java          Batch processing and data quality validation

src/test/java/com/securevision/
├── SolidPrinciplesTest.java     Task 4 tests for Task 1
├── SecurityTest.java            Task 4 tests for Task 2
└── PerceptionSystemTest.java    Task 4 tests for Task 3

.github/workflows/
└── devsecops-pipeline.yml       Task 2 - CI/CD pipeline with OWASP security scanning
```

---

## How to Build and Run Tests

### Prerequisites

- JDK 17 or higher
- Maven 3.8 or higher

### Build the project

```bash
mvn clean install
```

### Run all tests

```bash
mvn test
```

### Run OWASP security scan (SCA)

```bash
mvn org.owasp:dependency-check-maven:check
```

---

## CI/CD Pipeline (Task 2)

The GitHub Actions pipeline runs automatically on every push. It has 6 stages:

1. **Build** - compiles the source code
2. **Test** - runs all JUnit 5 tests with JaCoCo coverage
3. **SCA** - OWASP Dependency Check for vulnerable dependencies
4. **SAST** - static code analysis for security issues
5. **Security Gate** - evaluates all scan results before deploy
6. **Deploy** - runs only on main branch after all gates pass

---

## Technologies Used

| Technology             | Purpose                                         |
| ---------------------- | ----------------------------------------------- |
| Java 17                | Main programming language                       |
| Maven                  | Build and dependency management                 |
| JUnit 5                | Automated testing framework (Task 4)            |
| AssertJ                | Fluent test assertions (Task 4)                 |
| Mockito                | Mocking framework for unit tests                |
| Google Guava           | RateLimiter for brute force protection (Task 2) |
| OWASP Dependency Check | Real SCA security scanning (Task 2)             |
| JaCoCo                 | Code coverage reporting                         |
| GitHub Actions         | CI/CD pipeline automation (Task 2)              |
| SLF4J                  | Logging throughout the application              |

---

## ONCE Dataset (Task 3)

This project uses the **ONCE (One Million Scenes)** autonomous driving dataset for the perception pipeline implementation in Task 3.

The ONCE dataset contains:

- 1 million LiDAR scenes
- 7 million camera images
- 144 driving hours of data
- Annotations for Cars, Pedestrians, and Cyclists

**The dataset is NOT included in this repository due to its large size.**

### How to Download and Set Up the Dataset

1. Visit the official download page: https://once-for-auto-driving.github.io/download.html

2. Download the Training or Validation split (validation is smaller and recommended for testing):
   - Annotations (JSON files)
   - LiDAR data (.bin files)
   - Camera data (.jpg files)

3. Extract all tar files into a folder called `once_dataset/` in the project root:

   ```bash
   tar -xf train_infos.tar
   tar -xf train_lidar.tar
   tar -xf train_cam01.tar
   # ... repeat for other camera tar files
   ```

4. Your folder structure should look like:

   ```
   securevision-platform/
   ├── once_dataset/
   │   └── data/
   │       ├── 000000/
   │       │   ├── cam01/
   │       │   ├── lidar_roof/
   │       │   └── 000000.json
   │       ├── 000001/
   │       └── ...
   ├── src/
   └── pom.xml
   ```

5. Run the perception system demo:
   ```bash
   mvn compile
   mvn exec:java -Dexec.mainClass="com.securevision.perception.AutonomousPerceptionSystem"
   ```

### Dataset Citation

Mao et al. (2021) _One Million Scenes for Autonomous Driving: ONCE Dataset_. NeurIPS 2021.
