# CE316 Integrated Assignment Environment

A lightweight desktop application for managing and evaluating programming
assignment submissions. The application lets a lecturer define how an
assignment should be compiled or interpreted, run every submitted ZIP file with
that configuration, compare student output against an expected output file, and
view the result for each student.

This repository contains the working prototype for the CE316 project. The
prototype focuses on requirements 3, 4, 7, 8, and 9 from the project
description.

## Prototype Scope

| Requirement | Status | Implementation |
| --- | --- | --- |
| Requirement 3 | Implemented | Create a project and attach an existing or newly created configuration. |
| Requirement 4 | Implemented | Create, edit, save, and remove reusable configurations. |
| Requirement 7 | Implemented | Compile or interpret submissions using the selected project configuration. |
| Requirement 8 | Implemented | Compare actual program output with an expected output file. |
| Requirement 9 | Implemented | Display each student's result in the application UI. |

## Features

- JavaFX desktop interface for project setup and evaluation.
- Reusable assignment configurations stored in a local SQLite database.
- Default configuration templates for C, C++, Java, and Python.
- ZIP discovery from a selected submissions folder.
- Recursive source file lookup inside extracted submissions.
- Compile, run, timeout, runtime error, and output mismatch reporting.
- Result table with per-student status and detailed error/output messages.
- Project save/open support through a file-based SQLite database.

## Tech Stack

- Java 21
- JavaFX 21
- Maven
- SQLite via `sqlite-jdbc`

The application is standalone and does not require a database server.

## Repository Structure

```text
src/main/java/com/iae
├── controller      JavaFX UI controller
├── model           Project, configuration, and result models
├── repository      SQLite persistence layer
├── service         Configuration, project, file, and execution services
├── Main.java       JavaFX application entry point
└── Launcher.java   Jar launcher entry point

src/main/resources
├── fxml/main.fxml  Main JavaFX layout
└── css/styles.css  Application styling
```

## Getting Started

### Prerequisites

Install Java 21 or newer:

```bash
java -version
```

Install Maven:

```bash
mvn -version
```

### Run from Source

```bash
mvn javafx:run
```

### Build the Jar

```bash
mvn package
```

The packaged jar is created at:

```text
target/ce316-iae-1.0.0.jar
```

Run it with:

```bash
java -jar target/ce316-iae-1.0.0.jar
```

> Note: JavaFX includes platform-specific native libraries. If a release jar was
> built on another operating system, build the jar locally with `mvn package`
> for the best compatibility.

## Release

The current prototype release is available here:

[CE316 IAE v1.0.0](https://github.com/utkubilir/CE316-Project/releases/tag/v1.0.0)

Direct jar download:

[ce316-iae-1.0.0.jar](https://github.com/utkubilir/CE316-Project/releases/download/v1.0.0/ce316-iae-1.0.0.jar)

## Demo Workflow

1. Start the application.
2. Create a new project from **File > New Project**.
3. Select an existing configuration or create a new one.
4. Choose the folder that contains student ZIP submissions.
5. Select the expected output file.
6. Click **Run Tests**.
7. Review each student's result in the **Test Results** table.

## Submission ZIP Format

Each student submission should be a ZIP file. The application uses the ZIP file
name as the student ID. For example:

```text
submissions/
├── 20240001.zip
├── 20240002.zip
└── 20240003.zip
```

Inside each ZIP, the expected source file should match the selected
configuration, such as `main.c`, `Main.java`, or `main.py`.

## Configuration Examples

### C

```text
Source file:     main.c
Compile command: gcc main.c -o main.exe
Run command:     main.exe
```

### Java

```text
Source file:     Main.java
Compile command: javac Main.java
Run command:     java Main
```

### Python

```text
Source file:     main.py
Compile command:
Run command:     python main.py
```

## Data Storage

The application stores projects, configurations, and saved results in a local
SQLite database:

```text
iae_projects.db
```

The database is created or reused in the directory where the application is
started.

## Current Prototype Limitations

- Configuration import/export is not part of the current prototype scope.
- Windows installer packaging is not part of the current prototype scope.
- The manual/help page is not part of the current prototype scope.
- For final deployment, build and test the jar on the target operating system.
