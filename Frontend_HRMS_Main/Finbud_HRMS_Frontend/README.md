# Finbud Financial — HRMS Frontend

A production-grade Human Resource Management System (HRMS) frontend built for **Finbud Financial**. This React + TypeScript application provides role-based dashboards for Admin, HR, Team Leaders, and Employees to manage attendance, payroll, leaves, departments, shifts, and more.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Roles & Access](#roles--access)
- [Features by Role](#features-by-role)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Available Scripts](#available-scripts)
- [Key Pages & Routes](#key-pages--routes)
- [State Management](#state-management)
- [API Layer](#api-layer)
- [Component Architecture](#component-architecture)
- [Authentication Flow](#authentication-flow)
- [AI Assistant](#ai-assistant)
- [Deployment](#deployment)
- [Backend Integration](#backend-integration)

---

## Overview

Finbud HRMS replaces manual HR processes — Excel timesheets, paper leave forms, and error-prone payroll calculations — with a fully automated, role-gated digital workspace.

**Key highlights:**

- Role-based routing and UI rendering (Admin / HR / Team Leader / Employee)
- Fingerprint-ready attendance dashboard with punch-in/out records
- Leave management with application workflow and balance tracking
- Payroll viewer with payslip preview per employee
- Admin announcement center visible across all role dashboards
- AI-powered HR assistant (chat panel powered by backend/OpenAI)
- Leaderboard based on disbursal performance
- Audit log timeline for compliance tracking
- One-time employee self-service profile edit with HR override

---

## Tech Stack

| Category           | Library / Tool                          |
|--------------------|-----------------------------------------|
| Framework          | React 18                                |
| Language           | TypeScript 5                            |
| Build Tool         | Vite 5                                  |
| Styling            | Tailwind CSS 3                          |
| Routing            | React Router DOM v6                     |
| Server State       | TanStack React Query v5                 |
| Client State       | Zustand v5 (with `persist` middleware)  |
| HTTP Client        | Axios                                   |
| Icons              | Lucide React                            |
| Class Utility      | clsx                                    |
| Linting            | ESLint                                  |

---

## Project Structure

```
src/
├── components/
│   ├── common/            # Shared UI components (DataTable, FilterBar, StatusBadge, PageHeader, etc.)
│   └── layout/            # Sidebar, Topbar
│   └── ui/                # Base UI primitives (Button, Card, Input, Select)
│
├── constants/
│   ├── mockData.ts        # Seed data for employees, attendance, payroll, leaves, etc.
│   └── navigation.ts      # Role-filtered nav items
│
├── features/
│   ├── assistant/         # AI HR assistant panel
│   ├── attendance/        # Attendance summary component
│   ├── audit/             # Audit timeline component
│   ├── dashboard/         # Role-specific dashboards (Admin, HR, TeamLeader, Employee)
│   ├── departments/       # Department form
│   ├── employee/          # Employee form
│   ├── leave/             # Leave calendar
│   ├── payroll/           # Payslip preview
│   ├── reports/           # Report cards
│   └── shifts/            # Shift form
│
├── hooks/
│   └── useRoleAccess.ts   # Role guard hook
│
├── layouts/
│   └── AppLayout.tsx      # Main layout with Sidebar + Topbar + Outlet
│
├── pages/                 # One file per route
│   ├── LoginPage.tsx
│   ├── DashboardPage.tsx
│   ├── EmployeesPage.tsx
│   ├── EmployeeProfilePage.tsx
│   ├── AttendancePage.tsx
│   ├── LeavePage.tsx
│   ├── PayrollPage.tsx
│   ├── ShiftsPage.tsx
│   ├── DepartmentsPage.tsx
│   ├── ReportsPage.tsx
│   ├── AuditLogsPage.tsx
│   ├── LeaderboardPage.tsx
│   ├── EmployeeDailyCommitmentPage.tsx
│   ├── EmployeeHourlyUpdatesPage.tsx
│   ├── EmployeeMonthlyTargetsPage.tsx
│   ├── EmployeePayrollPage.tsx
│   ├── TeamLeaderDailyCommitmentPage.tsx
│   ├── TeamLeaderMonthlyCommitmentPage.tsx
│   ├── TeamLeaderPayrollPage.tsx
│   └── TeamLeaderReportsPage.tsx
│
├── routes/
│   ├── AppRouter.tsx      # Lazy-loaded route definitions
│   └── ProtectedRoute.tsx # Role-based route guard
│
├── services/
│   ├── api.ts             # Mock API with simulated delay
│   └── http.ts            # Axios instance with JWT interceptor
│
├── store/
│   ├── authStore.ts       # Auth state (Zustand + persist)
│   ├── adminStore.ts      # Admin monthly target (Zustand + persist)
│   ├── uiStore.ts         # UI state (assistant panel open/close)
│   └── index.ts
│
├── types/
│   └── index.ts           # Shared TypeScript interfaces
│
└── utils/
    ├── cn.ts              # Tailwind class merging
    └── format.ts          # formatCurrency, getInitials
```

---

## Roles & Access

| Role         | Demo Login Email              | Scope                                    |
|--------------|-------------------------------|------------------------------------------|
| Admin        | admin@finbud.com              | Full access + announcement publishing   |
| HR           | ishita.rao@finbud.com         | Employees, payroll, attendance, leaves  |
| Team Leader  | rohan.mehta@finbud.com        | Own team only, read-only payroll        |
| Employee     | aanya.sharma@finbud.com       | Self-service, own data only             |

> All passwords are `password` in demo mode. The login page auto-fills credentials when a role is selected.

---

## Features by Role

### Admin
- Command dashboard with org-wide performance metrics
- Publish announcements visible to all roles
- Set monthly rupee target broadcast to all dashboards
- View full employee performance table

### HR
- HR operations dashboard (headcount, attendance, leaves, payroll KPIs)
- Create / edit / delete employees
- Manage payroll (edit restricted to HR)
- Approve or reject leave requests
- Shift and department management
- Access to reports and audit logs

### Team Leader
- See only own team members (scoped by `teamLeader` field)
- Team attendance view (today's punch records)
- Set daily and monthly team commitments
- View team performance and disbursal reports
- Read-only access to own payroll (Leadership L2)

### Employee
- Personal dashboard with daily commitment and monthly targets
- Attendance view (fingerprint-based, read-only)
- Apply for leave; view leave balance
- Submit hourly task updates
- View own payroll (read-only salary structure)
- One-time self-service profile edit (locked after first use)
- Leaderboard participation

---

## Getting Started

### Prerequisites

- Node.js `>= 18.0.0`
- npm `>= 8.0.0`

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/finbud/finbud-hrms-frontend.git
cd finbud-hrms-frontend

# 2. Install dependencies
npm install

# 3. Start the development server
npm run dev
```

The app will be available at `http://localhost:5173`.

---

## Environment Variables

Create a `.env` file in the project root:

```env
# Base URL for the Spring Boot backend
VITE_API_BASE_URL=http://localhost:8080

# Optional: used for analytics or monitoring
VITE_APP_ENV=development
```

> The mock API in `src/services/api.ts` is active by default. To switch to real API calls, replace mock implementations with `http` (Axios) calls from `src/services/http.ts`.

---

## Available Scripts

```bash
# Start development server (hot reload)
npm run dev

# Type-check + production build
npm run build

# Preview production build locally
npm run preview

# Lint all TypeScript/TSX files
npm run lint
```

---

## Key Pages & Routes

| Route                         | Roles Allowed                          | Description                              |
|-------------------------------|----------------------------------------|------------------------------------------|
| `/`                           | All                                    | Role-specific dashboard                  |
| `/login`                      | Public                                 | Demo role selector + login               |
| `/employees`                  | Admin, HR, Team Leader                 | Employee list with search/filter         |
| `/employees/:employeeId`      | Admin, HR, Team Leader                 | Employee profile + audit trail           |
| `/my-profile`                 | Employee                               | Self-service profile (one-time edit)     |
| `/attendance`                 | All                                    | Attendance records (scoped by role)      |
| `/leave`                      | All                                    | Leave application + approval queue       |
| `/shifts`                     | Admin, HR                              | Shift creation and assignment            |
| `/payroll`                    | Admin, HR                              | Payroll table + payslip preview          |
| `/departments`                | Admin, HR                              | Department management                    |
| `/reports`                    | Admin, HR                              | Report generation (PDF/Excel/CSV)        |
| `/audit-logs`                 | Admin, HR                              | Activity timeline                        |
| `/leaderboard`                | All                                    | Disbursal performance ranking            |
| `/daily-commitment`           | Employee                               | Set daily targets                        |
| `/hourly-updates`             | Employee                               | Log hourly task progress                 |
| `/monthly-targets`            | Employee                               | View monthly goals                       |
| `/employee-payroll`           | Employee                               | Read-only payroll summary                |
| `/team-daily-commitment`      | Team Leader                            | Team daily goal settings                 |
| `/team-monthly-commitment`    | Team Leader                            | Team monthly commitment plan             |
| `/team-payroll`               | Team Leader                            | Own payroll (read-only)                  |
| `/team-reports`               | Team Leader                            | Team performance reports                 |

---

## State Management

### Zustand Stores

| Store          | File                    | Persisted | Purpose                                              |
|----------------|-------------------------|-----------|------------------------------------------------------|
| `authStore`    | `store/authStore.ts`    | Yes       | Current user, role, self-service edit flag           |
| `adminStore`   | `store/adminStore.ts`   | Yes       | Monthly target amount set by Admin                   |
| `uiStore`      | `store/uiStore.ts`      | No        | AI assistant panel open/close state                  |

### TanStack React Query

Used for all server-state fetching with 5-minute stale time and no refetch-on-window-focus. Query keys follow the pattern `['resource-name']`.

---

## API Layer

### Mock API (`src/services/api.ts`)

All API functions return mock data with a 250ms simulated delay:

```ts
api.employees.list()          // → Employee[]
api.employees.getById(id)     // → Employee | null
api.attendance.list()         // → AttendanceRecord[]
api.shifts.list()             // → Shift[]
api.leave.list()              // → LeaveRequest[]
api.payroll.list()            // → PayrollEntry[]
api.departments.list()        // → Department[]
api.audit.list()              // → AuditLog[]
```

### HTTP Client (`src/services/http.ts`)

Axios instance configured with:
- `baseURL` from `VITE_API_BASE_URL`
- 15s timeout
- Request interceptor that injects `Authorization: Bearer <token>` from `localStorage`

To wire real backend calls, replace mock functions in `api.ts` with `http.get(...)` / `http.post(...)` calls.

---

## Component Architecture

### Base UI Primitives (`src/components/ui/`)

| Component  | Props                          | Notes                              |
|------------|--------------------------------|------------------------------------|
| `Button`   | `variant`, `...HTMLButtonProps` | primary / secondary / ghost / danger |
| `Card`     | `className`, `...HTMLDivProps` | Frosted glass surface              |
| `Input`    | `label`, `...HTMLInputProps`   | Labeled input with focus ring      |
| `Select`   | `label`, `options[]`           | Labeled native select              |

### Common Components (`src/components/common/`)

| Component       | Purpose                                                  |
|-----------------|----------------------------------------------------------|
| `DataTable`     | Generic typed table with column renderer functions       |
| `FilterBar`     | Grid container for filter controls                       |
| `PageHeader`    | Title + description + optional action button             |
| `StatusBadge`   | Pill badge with tone (success / warning / danger / info) |
| `StatsCard`     | KPI card with icon, value, and meta text                 |
| `AuditTimeline` | Vertical timeline of audit log entries                   |
| `ConfirmModal`  | Accessible delete/confirm dialog                         |
| `EmptyState`    | Placeholder for empty data states                        |
| `LoadingSkeleton` | Animated pulse skeleton for loading states             |

---

## Authentication Flow

```
LoginPage
  └── loginAsRole(role)          ← Zustand authStore
        └── Sets user + isAuthenticated = true
              └── Navigate to "/"
                    └── DashboardPage
                          └── Renders AdminDashboard | HRDashboard | TeamLeaderDashboard | EmployeeDashboard
```

**ProtectedRoute** reads `user.role` from `authStore` and:
- Redirects to `/login` if not authenticated
- Renders `<UnauthorizedPage />` if role not in allowed list
- Renders `<Outlet />` if authorized

**JWT Flow (production):**

1. `POST /api/auth/login` → receives `accessToken` + `refreshToken`
2. `accessToken` stored in `localStorage` as `finbud_access_token`
3. Axios interceptor in `http.ts` injects it on every request
4. On 401, trigger refresh token flow via `POST /api/auth/refresh-token`

---

## AI Assistant

The floating assistant panel (`AssistantPanel.tsx`) is always available via the Topbar. In the current build it returns a simulated response. To connect to the real backend:

```ts
// Replace the simulated response block in AssistantPanel.tsx with:
const res = await http.post('/api/ai/query', { query: draft });
const aiMessage = res.data.data.response;
```

Backend endpoint: `POST /api/ai/query` (expects `{ query: string, context?: string }`)

---

## Deployment

### Build for Production

```bash
npm run build
# Output in /dist
```

### Serve with Nginx (example)

```nginx
server {
  listen 80;
  root /var/www/finbud-hrms;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://localhost:8080;
  }
}
```

### Docker (optional)

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## Backend Integration

This frontend is designed to connect with the **Finbud HRMS Spring Boot backend**.

| Frontend Service Call             | Backend Endpoint                          |
|-----------------------------------|-------------------------------------------|
| `api.employees.list()`            | `GET /api/employees?page=0&size=20`       |
| `api.employees.getById(id)`       | `GET /api/employees/{id}`                 |
| `api.attendance.list()`           | `GET /api/attendance/employee/{id}`       |
| `api.leave.list()`                | `GET /api/leaves/employee/{id}`           |
| `api.payroll.list()`              | `GET /api/payroll/employee/{id}`          |
| `api.departments.list()`          | `GET /api/departments`                    |
| `api.shifts.list()`               | `GET /api/shifts`                         |
| `api.audit.list()`                | `GET /api/audit-logs`                     |
| Login                             | `POST /api/auth/login`                    |
| Register                          | `POST /api/auth/register`                 |
| AI Query                          | `POST /api/ai/query`                      |

For full API documentation, access Swagger UI at: `http://localhost:8080/swagger-ui.html`

---

## License

Private License — Finbud Financial. All rights reserved.

---

## Support

For support, contact [support@financebuddha.com](mailto:support@financebuddha.com)
