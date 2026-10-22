# MediBook

MediBook is a hospital appointment booking application that helps patients find doctors and manage visits, while giving doctors and administrators tools to manage appointments and provider profiles.

## Features

- Patient and doctor accounts with role-specific dashboards
- Doctor directory with department filtering
- Appointment booking and status management
- Doctor schedules and availability
- Administrator tools for managing doctors, patients, and appointments
- Optional AI symptom triage using Google Cloud Vertex AI

## Technology

- Java 17+
- Spring Boot and Spring Security with JWT
- Spring Data JPA and Hibernate
- MySQL, with file-backed H2 available for local development
- HTML, CSS, and JavaScript

## Run locally

1. Install Java 17 or newer and Maven 3.9 or newer.
2. Open PowerShell in the project directory.
3. Start the application:

   ```powershell
   mvn spring-boot:run
   ```

4. Open [http://localhost:8080](http://localhost:8080).

The app uses a local H2 database by default. To configure MySQL, administrator accounts, demo doctors, or optional Vertex AI triage, follow [SETUP.md](./SETUP.md).

## Notes

- Doctor accounts created through signup require administrator approval before appearing in the patient directory.
- AI triage is optional and informational; it is not a medical diagnosis.
- Do not commit passwords, API keys, cloud credentials, or other secrets to the repository.
