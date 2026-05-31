# CE316 Integrated Assignment Environment (IAE)

A comprehensive JavaFX desktop application designed to automate and simplify the grading of programming assignments. The application allows lecturers to extract student submissions, compile and run their code safely, compare outputs against expected results, and generate detailed grading reports.

## Key Features & Implemented Requirements

All assignment requirements have been fully successfully implemented:

- **Automated Extraction**: Safely extracts student ZIP files, automatically resolving nested directories to locate source files while actively preventing Zip-Slip vulnerability attacks.
- **Project & Configuration Management**: Create projects and manage language configurations. Built-in, zero-setup support for **C, C++, Java, and Python**.
- **Compilation & Execution Engine**: Compiles (if necessary) and runs student code against a predefined expected output file.
- **Robust Evaluation & Security**:
  - Accurately detects and handles **Compile Errors**, **Runtime Errors**, and **Missing Sources**.
  - Enforces a **10-second Timeout** to immediately kill infinite loops.
  - Normalizes outputs (ignores minor whitespace/newline/CRLF differences) to ensure fair comparisons.
- **Interactive Grading & Code Editor**:
  - Automatically assigns a **0-100 grade** to submissions based on their execution status tier.
  - Built-in monospaced **Source Code Editor**. You can view a student's code, fix a minor syntax error or typo, and click **Save & Re-evaluate** to instantly re-grade them on the spot!
- **Data Isolation & Portability**: Uses a persistent SQLite database. All temporary extractions, databases, and logs are isolated within the user's `%APPDATA%\CE316-IAE` (or `~/.ce316-iae`) directory, fully supporting read-only deployment locations like `Program Files`.
- **Exportable Reports**: Exports beautifully formatted, standalone HTML reports summarizing project statistics, average grades, and individual student execution logs.

## Tech Stack

- **Java 21**
- **JavaFX 21**
- **Maven**
- **SQLite**

## Running Locally (Development)

Run the application directly using Maven:

```bash
mvn clean javafx:run
```

## Windows Installer (.MSI)

We provide a script to generate a fully bundled Native Windows Installer (`.msi`). This installer bundles a private Java Runtime (JRE), so the end-user **does not** need to have Java installed on their computer to run the app.

1. Open PowerShell and run the build script:
```powershell
.\scripts\build-installer.ps1
```
2. The script will automatically compile the project, package it, and run the `jpackage` tool.
3. The final `.msi` file will be generated inside the `target/installer/` directory.

*(Note: The WiX Toolset v3 is required for MSI generation. If it is missing, the script will guide you on how to install it via Winget.)*

## Basic Usage Guide

1. Open the application.
2. Click **File → New Project** and enter a project name.
3. Select an existing language configuration (e.g., *Java Configuration*) or create a custom one.
4. In the sidebar, browse and select the folder containing your students' `.zip` submissions.
5. Browse and select your `expected_output.txt` file.
6. Click **Run Tests**.
7. Double-click any student in the results table to view their compilation logs or edit their source code directly!
8. Click **Export Report** to save the grades as an HTML document.
