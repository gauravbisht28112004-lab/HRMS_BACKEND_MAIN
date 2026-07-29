# Finbud HRMS - System Architecture

## Overview

Finbud HRMS is a production-grade Human Resource Management System built for Finbud Financial to automate HR processes including employee management, attendance tracking, leave management, and payroll processing.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    CLIENT LAYER                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │  Web App    │  │ Mobile App  │  │ Fingerprint │  │  HR Portal  │  │   Reports   │   │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘   │
│         └─────────────────┴─────────────────┴─────────────────┴─────────────────┘      │
│                                     │                                                   │
│                              REST API / HTTPS                                         │
│                                     │                                                   │
└─────────────────────────────────────┼───────────────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────┼───────────────────────────────────────────────────┐
│                              API GATEWAY LAYER                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐     │
│  │                    Spring Cloud Gateway (Rate Limiting, Routing)                 │     │
│  └─────────────────────────────────────────────────────────────────────────────────┘     │
│                                    │                                                    │
└────────────────────────────────────┼────────────────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────────────────────┐
│                           APPLICATION LAYER (Spring Boot)                               │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │                         SECURITY LAYER                                           │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │  │
│  │  │   JWT Auth  │  │    RBAC     │  │ Rate Limiter│  │  CORS Config│            │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │                      CONTROLLER LAYER (REST APIs)                                │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │  │
│  │  │ Employee │ │Attendance│ │  Shift   │ │  Leave   │ │ Payroll  │ │Department│ │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                           │  │
│  │  │  Report  │ │   AI     │ │  Auth    │ │  Audit   │                           │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘                           │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │                      SERVICE LAYER (Business Logic)                            │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │  │
│  │  │ Employee │ │Attendance│ │  Shift   │ │  Leave   │ │ Payroll  │ │Department│   │  │
│  │  │ Service  │ │  Engine  │ │ Service  │ │ Service  │ │ Engine   │ │ Service  │   │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘    │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                              │  │
│  │  │  Report  │ │   AI     │ │  Audit   │ │  Email   │                              │  │
│  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │                              │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘                              │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │                      REPOSITORY LAYER (Spring Data JPA)                          │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │  │
│  │  │ Employee │ │Attendance│ │  Shift   │ │  Leave   │ │  Payroll │ │Department│   │  │
│  │  │   Repo   │ │   Repo   │ │   Repo   │ │   Repo   │ │   Repo   │ │   Repo   │   │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘   │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                             │  │
│  │  │  User    │ │   Role   │ │  Audit   │ │ Vector   │                             │  │
│  │  │   Repo   │ │   Repo   │ │   Repo   │ │   Repo   │                             │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘                             │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │                      SCHEDULER LAYER (Cron Jobs)                                 │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                             │  │
│  │  │ Payroll  │ │Attendance│ │   Leave  │ │   AI     │                             │  │
│  │  │  Gen     │ │  Daily   │ │ Balance  │ │  Sync    │                             │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘                             │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────┼────────────────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────────────────────┐
│                            DATA LAYER                                                    │
│                                                                                          │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐              │
│  │    PostgreSQL       │  │      Redis          │  │      pgvector       │              │
│  │  (Primary Database) │  │  (Cache + Sessions) │  │  (Vector Search)    │              │
│  │                     │  │                     │  │                     │              │
│  │  • All entities     │  │  • Session store    │  │  • AI embeddings    │              │
│  │  • JSON columns     │  │  • Rate limiting    │  │  • Semantic search  │              │
│  │  • Audit logs       │  │  • Cache layer      │  │  • RAG storage      │              │
│  │  • Full-text search │  │  • Pub/Sub          │  │                     │              │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘              │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                         External Services                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │   OpenAI    │  │   Email     │  │  Fingerprint│  │    PDF      │              │   │
│  │  │    API      │  │   Service   │  │   Device    │  │  Generator  │              │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘              │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| Language | Java 21 | Modern features, improved performance |
| Framework | Spring Boot 3.2 | Core application framework |
| Security | Spring Security + JWT | Authentication & Authorization |
| ORM | Spring Data JPA | Database abstraction |
| Database | PostgreSQL 16 | Primary relational database |
| Vector DB | pgvector extension | AI embeddings storage |
| Cache | Redis | Session management, caching |
| AI | OpenAI API + Embeddings | Smart HR assistant |
| Messaging | Spring Scheduler | Cron jobs, async processing |
| Documentation | OpenAPI 3.0 | API documentation |
| Build | Maven | Dependency management |
| Container | Docker + Docker Compose | Containerization |

## Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    REQUEST FLOW                             │
└─────────────────────────────────────────────────────────────┘

Request → CORS Filter → Rate Limiter → JWT Filter → RBAC Filter → Endpoint
                                              ↓
                                         Role Check
                                              ↓
                                    Admin / HR / Manager / Employee
```

### JWT Token Structure
```json
{
  "sub": "user-id",
  "email": "user@finbud.com",
  "roles": ["ROLE_HR", "ROLE_MANAGER"],
  "permissions": ["EMPLOYEE_READ", "PAYROLL_READ"],
  "iat": 1712412345,
  "exp": 1712415945
}
```

## Module Dependencies

```
Employee Management
    ↓
Department Management ← → Shift Management
    ↓                           ↓
Attendance System ← → Leave Management
    ↓                           ↓
        Payroll Engine
            ↓
        AI Service (uses all modules)
```

## Scalability Considerations

1. **Horizontal Scaling**: Stateless services enable easy horizontal scaling
2. **Database**: Connection pooling, read replicas for reporting
3. **Caching**: Redis for frequently accessed data
4. **Async Processing**: @Async for email, PDF generation
5. **File Storage**: Separate storage service for documents

## Monitoring & Observability

- Structured logging with correlation IDs
- Health checks endpoints
- Metrics collection (Micrometer)
- API response time tracking
- Database query performance monitoring

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     KUBERNETES CLUSTER                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │   Ingress   │ │   Config    │ │      Services           │ │
│  │   Controller│ │     Map     │ │ ┌─────┐ ┌─────┐ ┌────┐ │ │
│  └─────────────┘ └─────────────┘ │ │API  │ │API  │ │API │ │ │
│         │                        │ │Pod 1│ │Pod 2│ │Pod3│ │ │
│         └────────────────────────┘ └─────┘ └─────┘ └────┘ │ │
│                                  └─────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              PostgreSQL (StatefulSet)                    │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Redis (StatefulSet)                         │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```
