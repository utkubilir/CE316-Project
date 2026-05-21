# CE316 Integrated Assignment Environment

A JavaFX desktop prototype for managing and testing programming assignment
submissions.

The application lets a lecturer create a project, select or create a
configuration, process student ZIP files, run each submission, compare the
output with the expected output, and view the result for every student.

## Implemented Requirements

- Requirement 3: Create a project with an existing or new configuration.
- Requirement 4: Create, edit, and remove configurations.
- Requirement 7: Compile or interpret source code using the project configuration.
- Requirement 8: Compare student output with the expected output.
- Requirement 9: Display each student's result.

## Tech Stack

- Java 21
- JavaFX 21
- Maven
- SQLite

## Run from Source

```bash
mvn javafx:run
```

## Build and Run Jar

```bash
mvn package
java -jar target/ce316-iae-1.0.0.jar
```

Java 21 or newer is required.

## Windows Installer

To create a Windows installer:
1. Build the jar file using `mvn clean package`.
2. Open the `installer.iss` script with [Inno Setup](https://jrsoftware.org/isinfo.php).
3. Compile the script to generate the `CE316-IAE-Installer.exe` inside the `installer` directory.


## Release

Prototype release:

[CE316 IAE v1.0.0](https://github.com/utkubilir/CE316-Project/releases/tag/v1.0.0)

Direct jar download:

[ce316-iae-1.0.0.jar](https://github.com/utkubilir/CE316-Project/releases/download/v1.0.0/ce316-iae-1.0.0.jar)

## Basic Usage

1. Open the application.
2. Create a new project.
3. Select an existing configuration or create a new one.
4. Select the folder that contains student ZIP files.
5. Select the expected output file.
6. Click **Run Tests**.
7. Check the results table.

## Notes

- Student submissions should be ZIP files.
- The ZIP file name is used as the student ID.
- Project and configuration data is stored locally in `iae_projects.db`.
- The release jar was built on macOS; for another operating system, build the jar locally with `mvn package`.
