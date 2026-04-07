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

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/finbud_hrms

# JWT
app.jwt.secret=your-secret-key
app.jwt.expiration-ms=86400000

# OpenAI
openai.api.key=your-openai-api-key

# Scheduling
app.scheduler.payroll.cron=0 0 1 1 * ?
app.scheduler.attendance.cron=0 59 23 * * ?
```

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
