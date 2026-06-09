# Finbud HRMS - Enterprise Human Resource Management System

A production-grade HRMS backend built for Finbud Financial to automate HR processes including employee management, attendance tracking, leave management, payroll processing, and AI-powered HR analytics.

## Features

### 1. Employee Management
- Complete employee lifecycle management
- Department and hierarchy management
- Shift assignments
- Document management (PAN, Aadhaar, etc.)
- Search, filter, and pagination

### 2. Attendance System (Fingerprint Based)
- Fingerprint device integration
- Punch in/out tracking
- Automatic working hours calculation
- Late coming and early leaving detection
- Overtime calculation
- Half-day detection

### 3. Shift Management
- Multiple shift types (General, Morning, Evening, Night)
- Custom shift creation
- Grace period configuration
- Employee shift assignments

### 4. Leave Management
- Multiple leave types (Casual, Sick, Paid, WFH, LOP)
- Leave application workflow
- Manager approval process
- Automatic leave balance management
- Leave calendar view

### 5. Payroll System
- Automatic payroll generation
- Salary component calculations (Basic, HRA, DA, Allowances)
- Deductions (PF, ESI, Professional Tax, LOP)
- Overtime pay calculation
- Payslip generation (PDF)
- Payroll approval workflow

### 6. AI Features (OpenAI)
- Smart HR assistant with natural language queries
- Vector search using pgvector
- RAG-based HR analytics
- Automated insights generation

### 7. Role-Based Access Control
- Four roles: Admin, HR, Manager, Employee
- Granular permission system
- JWT-based authentication

### 8. Reports
- Attendance reports
- Leave reports
- Payroll reports
- Export to Excel, CSV, PDF

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA |
| Database | PostgreSQL 16 with pgvector |
| Cache | Redis |
| AI | OpenAI API |
| Documentation | OpenAPI 3.0 (Swagger) |
| Build Tool | Maven |
| Container | Docker |

## Quick Start

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker)

### Running with Docker

```bash
# Clone the repository
git clone <repository-url>
cd finbud-hrms

# Set environment variables (optional)
export OPENAI_API_KEY="your-openai-api-key"

# Start all services
docker-compose up -d

# Access the application
# API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Running Locally

```bash
# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### API Documentation

Once running, access the Swagger UI at: http://localhost:8080/swagger-ui.html

## First-time setup: bootstrap admin + bulk employee import

On first boot against an empty database, `DataInitializer` auto-provisions the
two Finbud bootstrap accounts so HR can sign in and load the rest of the
organisation from the Noida master spreadsheet.

| Login username | Role       | Employee code | Initial password | Name         |
|----------------|------------|---------------|------------------|--------------|
| `nd33004`      | `ROLE_ADMIN` | `ND33004`   | `finbud@123`     | Akash Deep   |
| `nd33301`      | `ROLE_HR`    | `ND33301`   | `finbud@123`     | Anjali Bisht |

Both accounts are issued with `passwordChangedAt = null` so the LoginResponse
returns `mustChangePassword: true` on first login — the UI force-rotates the
password before allowing any navigation.

The default password is resolved at import time from
`system_config.auth.default_password` (seeded to `finbud@123` by Flyway V7).
Change it once per environment before running the import if you want a
different initial password.

### Bulk employee import (the 112 Active employees on the master sheet)

1. Start the backend (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`).
2. Sign in at the frontend as `nd33004` / `finbud@123`. You will be prompted
   to rotate the password immediately.
3. Navigate to **Admin → Import Employees** and upload the master sheet
   (e.g. `Noida Master Employee Data Mar - 2026.xlsx`). The Active Data tab
   is processed automatically; the Resigned tab is skipped unless you tick
   _"Include resigned employees"_.
4. The importer runs a two-pass upsert: pass 1 inserts/updates the Employee,
   Department, SalaryStructure, and a User account (with `ROLE_EMPLOYEE` and
   the default password); pass 2 resolves the reporting-manager references.
   Response summarises `inserted / updated / skipped / failed`.
5. Each imported employee can now log in with username = their employee code
   lowercased (`nd33177`, `nd33022`, ...) and password `finbud@123`. First
   login forces a rotation.
6. Team Leader / Manager / additional Admin grants are a **manual** step:
   HR opens the admin user-management screen and grants extra roles to the
   specific employees who need them. The public `/api/auth/register` endpoint
   never grants anything above `ROLE_EMPLOYEE` (C-1 security guarantee).

### curl equivalent (for CI / re-imports)

```bash
# 1. Log in and grab the access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"nd33004","password":"<rotated-password>"}' \
  | jq -r '.data.accessToken')

# 2. Dry-run preview — validates headers and rows without writing
curl -X POST "http://localhost:8080/api/admin/import/employees?dryRun=true" \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@./Noida Master Employee Data Mar - 2026.xlsx'

# 3. Real import — default options (skip resigned, create users)
curl -X POST "http://localhost:8080/api/admin/import/employees" \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@./Noida Master Employee Data Mar - 2026.xlsx'
```

### Resetting a bad import

If the first import run produced dirty data, the admin-only cleanup endpoint
wipes all imported employees while preserving the reserved Finbud codes
(`ND33004`, `ND33301`) and any linked active user accounts:

```bash
curl -X DELETE "http://localhost:8080/api/admin/import/employees/cleanup" \
  -H "Authorization: Bearer $TOKEN"
```

## Project Structure

```
finbud-hrms/
├── src/
│   ├── main/
│   │   ├── java/com/financebuddha/finbud/hrms/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST API controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── enums/           # Enum classes
│   │   │   ├── exception/       # Exception handling
│   │   │   ├── mapper/          # MapStruct mappers
│   │   │   ├── repository/      # Spring Data repositories
│   │   │   ├── scheduler/       # Scheduled tasks
│   │   │   ├── security/        # Security configuration
│   │   │   ├── service/         # Business logic
│   │   │   └── util/            # Utility classes
│   │   └── resources/
│   │       ├── db/migration/    # Database migrations
│   │       ├── application.properties
│   │       └── application-dev.properties
│   └── test/                    # Test classes
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Register
- `POST /api/auth/logout` - Logout
- `POST /api/auth/change-password` - Change password
- `POST /api/auth/refresh-token` - Refresh token

### Employees
- `GET /api/employees` - List all employees
- `POST /api/employees` - Create employee
- `GET /api/employees/{id}` - Get employee by ID
- `PUT /api/employees/{id}` - Update employee
- `DELETE /api/employees/{id}` - Delete employee
- `GET /api/employees/search` - Search employees

### Attendance
- `POST /api/attendance/punch-in` - Record punch in
- `POST /api/attendance/punch-out` - Record punch out
- `GET /api/attendance/employee/{id}` - Get employee attendance
- `GET /api/attendance/late-comers/{date}` - Get late comers

### Leaves
- `POST /api/leaves/apply` - Apply for leave
- `POST /api/leaves/{id}/approve` - Approve leave
- `POST /api/leaves/{id}/reject` - Reject leave
- `GET /api/leaves/balance/{employeeId}` - Get leave balance

### Payroll
- `POST /api/payroll/generate` - Generate payroll
- `GET /api/payroll/{id}` - Get payroll by ID
- `POST /api/payroll/{id}/approve` - Approve payroll
- `GET /api/payroll/summary` - Get payroll summary

## Security

The application uses JWT-based authentication with the following security measures:

- JWT tokens with configurable expiration
- Role-based access control
- Password encryption using BCrypt
- CORS configuration
- Rate limiting support

## Database Schema

The database schema includes the following main tables:

- `employees` - Employee information
- `departments` - Department details
- `shift_types` - Shift configurations
- `attendance` - Attendance records
- `leave_requests` - Leave applications
- `leave_balances` - Leave balance tracking
- `payroll` - Payroll records
- `salary_structures` - Salary configurations
- `users` - User accounts
- `roles` - Role definitions
- `audit_logs` - Audit trail

See `DATABASE_SCHEMA.md` for detailed schema documentation.

## Scheduled Tasks

- **Payroll Generation**: Runs at 1:00 AM on the 1st of every month
- **Attendance Processing**: Runs at 11:59 PM daily

## Configuration

All secrets and environment-specific values are sourced from environment
variables. The defaults in `application.properties` are tuned for local
development only — production must override every variable flagged below
as **required**.

### Environment variables — dev (`application.properties`)

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/finbud_hrms` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password (dev placeholder) |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | _(empty)_ | Redis password |
| `APP_JWT_SECRET` | _(dev-only default)_ | JWT signing key — **rotate for prod** |
| `APP_JWT_EXPIRATION_MS` | `86400000` (24 h) | Access-token TTL |
| `APP_JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 d) | Refresh-token TTL |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated CORS origins |
| `OPENAI_API_KEY` | _(empty)_ | Optional — AI assistant |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | _(empty)_ | SMTP credentials |

### Environment variables — prod (`application-prod.properties`)

The prod profile has **no defaults** for the variables below. Spring
fails fast at startup if any of them is missing — this is intentional.

| Variable | Required | Notes |
| --- | --- | --- |
| `DATABASE_URL` | Yes | JDBC URL, including `?sslmode=require` for managed DBs |
| `DATABASE_USERNAME` | Yes | DB user |
| `DATABASE_PASSWORD` | Yes | DB password |
| `APP_JWT_SECRET` | Yes | 64+ random bytes, base64-encoded. Generate with `openssl rand -base64 64` |
| `APP_CORS_ALLOWED_ORIGINS` | Yes | Exact frontend origin(s), e.g. `https://hrms.finbud.in` |
| `APP_JWT_EXPIRATION_MS` | No (default `3600000`) | 1 h access tokens — shorter blast radius than dev |
| `APP_JWT_REFRESH_EXPIRATION_MS` | No (default `604800000`) | 7 d refresh tokens |
| `OPENAI_API_KEY` | No | Required only if AI assistant is enabled |

### System config (`system_config` table)

Some settings live in the DB so ops can change them without a redeploy.
Canonical keys (seeded by Flyway `V5`):

| Key | Purpose | Finbud default |
| --- | --- | --- |
| `payroll.pf.employer_default` | Employer PF (INR, fixed) | `1950` |
| `payroll.pf.employee_default` | Employee PF (INR, fixed) | `1950` |
| `payroll.lwf.default` | LWF deduction (INR) | `0` |
| `payroll.tds.contract_rate_percent` | Contract TDS rate | `5.00` |
| `payroll.calc.precision_scale` | BigDecimal precision for intermediate math | `4` |
| `payroll.calc.output_scale` | BigDecimal precision for persisted values | `2` |
| `auth.default_password` | Default password for admin-provisioned users | `Welcome@123` |
| `auth.default_role` | Role granted when admin omits `roles` | `ROLE_EMPLOYEE` |
| `attendance.device.api_key` | Shared secret for fingerprint-device punch endpoints (blank = dev-mode bypass) | _(blank in dev)_ |
| `attendance.device.api_key_header` | Header name the device sends the key on | `X-Device-Api-Key` |

### Other fixed properties

```properties
# Scheduling (cron expressions — overridable per env if needed)
app.scheduler.payroll.cron=0 0 1 1 * ?
app.scheduler.attendance.cron=0 0 23 * * ?
```

## Profile pictures (S3 / MinIO)

Employee avatars are stored in an S3-compatible object store. Everything is
configured through the `finbud.storage.s3.*` properties, which can point at
real AWS S3 in prod and a local MinIO container in dev. The backend returns a
short-lived presigned GET URL whenever an employee record is loaded, so the
bucket itself can stay private.

### Dev setup with MinIO (recommended)

Add the following service to your existing `docker-compose.yml` (or run it
standalone with `docker run`):

```yaml
services:
  minio:
    image: minio/minio:latest
    container_name: finbud-minio
    ports:
      - "9000:9000"   # S3 API
      - "9001:9001"   # Web console
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
    volumes:
      - minio-data:/data

volumes:
  minio-data:
```

Start it (`docker compose up -d minio`) and — on first run — the backend will
auto-create the configured bucket (`finbud-hrms-avatars` by default) because
`finbud.storage.s3.auto-create-bucket=true`. You can also create it manually
via the MinIO console at `http://localhost:9001` (login
`minioadmin` / `minioadmin`) if you'd rather not give the app that privilege.

### Environment variables — storage

| Variable | Default | Purpose |
| --- | --- | --- |
| `FINBUD_S3_ENABLED` | `true` | Master switch — avatar endpoints return 400 when `false` |
| `FINBUD_S3_ENDPOINT` | `http://localhost:9000` | S3 endpoint. Leave blank (or unset) for real AWS S3 |
| `FINBUD_S3_REGION` | `us-east-1` | AWS region — MinIO ignores this but the SDK still requires a value |
| `FINBUD_S3_BUCKET` | `finbud-hrms-avatars` | Bucket name |
| `FINBUD_S3_ACCESS_KEY` | `minioadmin` | Access key (dev default). In prod, leave blank to use the default AWS credential chain (IAM role / profile / env) |
| `FINBUD_S3_SECRET_KEY` | `minioadmin` | Secret key — same guidance as above |
| `FINBUD_S3_PATH_STYLE` | `true` | Required for MinIO. Set `false` for real AWS S3 |
| `FINBUD_S3_PUBLIC_BASE_URL` | _(blank)_ | Optional rewrite for presigned URLs. Use when the backend talks to MinIO on one hostname (e.g. `http://minio:9000` inside Docker) and the browser talks to it on another (e.g. `http://localhost:9000`) |
| `FINBUD_S3_PRESIGN_TTL_SECONDS` | `3600` | Presigned URL TTL |
| `FINBUD_S3_MAX_UPLOAD_BYTES` | `5242880` (5 MB) | Maximum avatar size. Must be kept in sync with the frontend validator in `AvatarUpload.tsx` |
| `FINBUD_S3_ALLOWED_CONTENT_TYPES` | `image/jpeg,image/png,image/webp` | Comma-separated allow-list |
| `FINBUD_S3_AUTO_CREATE_BUCKET` | `true` | Whether the service should create the bucket on startup if it doesn't exist |

### Endpoints

| Method | Path | Who can call it |
| --- | --- | --- |
| `POST` | `/api/employees/{employeeCode}/avatar` (multipart `file`) | Admin, HR, **or** the employee themselves |
| `DELETE` | `/api/employees/{employeeCode}/avatar` | Admin, HR, **or** the employee themselves |

Both endpoints return the full `EmployeeResponse`. The `profilePictureUrl`
field on the response is overwritten with a freshly minted presigned GET URL
when the employee has an `avatarKey` set, so the frontend doesn't need to
know anything about S3 / MinIO — it just renders whatever URL the backend
hands it via `<Avatar>` / `<AvatarUpload>`.

### Production notes

1. Point `FINBUD_S3_ENDPOINT` at `https://s3.<region>.amazonaws.com`
   (or leave it blank to use the SDK default), and set
   `FINBUD_S3_PATH_STYLE=false`.
2. Prefer IAM roles over static keys — leave `FINBUD_S3_ACCESS_KEY` /
   `FINBUD_S3_SECRET_KEY` blank so the SDK's default credential chain is
   used.
3. Keep the bucket private. The presigned URLs cover read access; there's no
   need to enable public object ACLs.
4. Set a lifecycle rule on the `avatars/` prefix if you want to garbage-
   collect orphaned keys (the app deletes the previous key on replace and on
   `DELETE`, but belt-and-braces never hurts).

## Testing

```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify
```

## Deployment

### Docker Deployment

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Kubernetes Deployment

See `k8s/` directory for Kubernetes deployment manifests.

## Monitoring

The application includes Spring Boot Actuator endpoints for monitoring:

- `/actuator/health` - Health check
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

Private License - Finbud Financial

## Support

For support, contact support@financebuddha.com
