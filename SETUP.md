# MediBook

MediBook is a hospital appointment booking system built with HTML, CSS, and browser JavaScript; Spring Boot REST APIs; Spring Security with JWT; MySQL; and Spring Data JPA/Hibernate. Vertex AI Gemini triage is called by the Spring backend so Google credentials are never placed in browser code.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- MySQL 8 or newer
- Optional for AI triage: Google Cloud project with Vertex AI enabled and Application Default Credentials

## Run Locally (Windows PowerShell)

For MySQL, make sure the Windows service is running before starting the app. In an Administrator PowerShell window, run `Start-Service MySQL80` if `Get-Service MySQL80` reports `Stopped`; then verify port 3306 with `Test-NetConnection 127.0.0.1 -Port 3306` (`TcpTestSucceeded` should be `True`). If you use H2, skip this step.

Open a terminal in this directory, the one containing `pom.xml`. Create the database once:

```sql
CREATE DATABASE medibook;
```

The app starts with a file-backed H2 database by default so you can run locally without MySQL. Spring Boot detects the JDBC driver and SQL dialect from the selected database URL. If you want MySQL instead, set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` together before starting; setting only the username/password leaves H2 selected, and MySQL credentials will be rejected by H2. The initial administrator is created only when its variables are set and the account does not already exist. Use a password of at least 12 characters.

```powershell
# Optional: use MySQL instead of the default H2 dev database
$env:DATABASE_URL = "jdbc:mysql://localhost:3306/medibook?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DATABASE_USERNAME = "root"
$env:DATABASE_PASSWORD = "your-mysql-password"
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "a-unique-password-at-least-12-chars"
$env:DEMO_DOCTORS_ENABLED = "true"
$env:DEMO_DOCTOR_PASSWORD = "a-demo-password-at-least-12-chars"
$jwtBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
mvn spring-boot:run
```
Open `http://localhost:8080`. The first startup creates the database tables through Hibernate. Check the backend with:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Patients can register directly from the sign-in page. A doctor can choose Doctor on the signup form and submit profile details; signup opens the doctor dashboard immediately. The doctor remains inactive in the patient directory until an administrator activates the profile from the Doctors page. Admins can also create doctor accounts directly.

With `DEMO_DOCTORS_ENABLED=true`, startup creates or updates 25 fictional doctor accounts with distinct names across the listed departments and their profiles in the `users` and `doctors` SQL tables. Sample consultation fees are in Indian rupees (INR). The seed is idempotent and uses `DEMO_DOCTOR_PASSWORD` for these sample logins. Do not enable sample doctors in production; configure verified providers through the admin account instead. To inspect the rows in MySQL:

```sql
SELECT u.name, u.email, d.department, d.specialization
FROM users u JOIN doctors d ON d.user_id = u.id;
```
   
## Gemini Triage (Optional)

Set the Google Cloud project and authenticate locally before starting the app:

```powershell
$env:VERTEX_PROJECT_ID = "your-google-cloud-project-id"
$env:VERTEX_LOCATION = "us-central1"
gcloud auth application-default login
mvn spring-boot:run
```

Enable the Vertex AI API for that project. Triage is unavailable until configured; booking and the rest of the app work without it. Triage is informational and is not a medical diagnosis.

## API Testing with Postman

All APIs use JSON. Register and login are public; copy the returned `token` and set Postman's Authorization type to **Bearer Token** for protected routes. In Postman, set `baseUrl` to `http://localhost:8080`.

| Method | Path | Access |
|---|---|---|
| `GET` | `/api/health` | Public health check |
| `POST` | `/api/auth/register` | Public patient registration (`name`, `email`, `password`, `phone`) |
| `POST` | `/api/auth/register-doctor-request` | Public doctor signup; returns a doctor session, with directory visibility controlled by admin activation |
| `POST` | `/api/auth/login` | Public login (`email`, `password`) |
| `POST` | `/api/auth/login/phone` | Public login (`phone`, `password`); phone number must match exactly one account |
| `GET` | `/api/auth/me` | Authenticated profile |
| `GET` | `/api/doctors` | Public active doctor directory; optional `?department=Cardiology` |
| `POST` | `/api/appointments` | Patient; `doctorId`, `date` (`YYYY-MM-DD`), `timeSlot` (`HH:mm`), `symptoms` |
| `GET` | `/api/appointments/mine` | Patient's visits |
| `PATCH` | `/api/appointments/{id}/cancel` | Owning patient |
| `GET` | `/api/appointments/doctor` | Doctor's visits |
| `PATCH` | `/api/appointments/{id}/status` | Assigned doctor or admin; `status`, optional `notes` |
| `GET` / `PUT` | `/api/doctors/me` and `/api/doctors/me/schedule` | Doctor profile and schedule |
| `GET` | `/api/admin/stats`, `/api/admin/users`, `/api/admin/patients`, `/api/admin/doctors`, `/api/admin/appointments` | Admin |
| `POST` | `/api/admin/doctors` | Admin creates a doctor account and profile |
| `PATCH` | `/api/doctors/{id}/active` | Admin enables/disables a doctor |
| `POST` | `/api/triage` | Authenticated; `symptoms` |

## Project Structure

```text
hospital-booking/
├── pom.xml
├── src/main/java/com/medibook/
│   ├── admin/             Admin REST endpoints
│   ├── api/               Health endpoint and API error responses
│   ├── appointment/       Appointment entity, repository, and booking APIs
│   ├── auth/              Registration, login, and current-user APIs
│   ├── doctor/            Doctor entity, repository, and schedule APIs
│   ├── security/          JWT filter, token service, and Spring Security
│   ├── triage/            Vertex AI Gemini service and REST endpoint
│   └── user/              User entity, roles, repository, admin bootstrap
├── src/main/resources/
│   ├── application.properties
│   └── static/            HTML, CSS, and JavaScript served by Spring Boot
└── SETUP.md
```

The old React-specific `components`, `contexts`, `pages`, `services`, and `types` folders are not part of this stack; any empty remnants can be ignored. The browser app is served by Spring Boot at the same origin as its REST APIs.

## Deployment

For Render, Railway, or AWS, build with `mvn clean package` and run `java -jar target/hospital-booking-1.0.0.jar`. Configure `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, and `ADMIN_EMAIL`/`ADMIN_PASSWORD` as hosting secrets. For Vertex AI, also configure `VERTEX_PROJECT_ID`, `VERTEX_LOCATION`, and workload identity/service-account credentials. Do not use the local placeholder JWT secret in a deployed environment.