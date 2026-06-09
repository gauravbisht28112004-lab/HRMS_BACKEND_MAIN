import type {
  AttendanceRecord,
  Department,
  Employee,
  Announcement,
  DailyCommitment,
  HourlyUpdate,
  LeaderboardEntry,
  LeaveBalance,
  MonthlyTarget,
  LeaveRequest,
  OfficeLocation,
  PayrollEntry,
  PublicHoliday,
  Regularization,
  Shift,
  ShiftAssignment,
} from '@/types';
import {
  type BackendAttendanceResponse,
  type BackendDepartmentResponse,
  type BackendEmployeeResponse,
  type BackendAnnouncementResponse,
  type BackendDailyCommitmentResponse,
  type BackendHourlyUpdateResponse,
  type BackendLeaderboardEntryResponse,
  type BackendLeaveBalanceResponse,
  type BackendMonthlyTargetResponse,
  type BackendLeaveResponse,
  type BackendLoginResponse,
  type BackendOfficeLocationResponse,
  type BackendPayrollResponse,
  type BackendPublicHolidayResponse,
  type BackendRegularizationResponse,
  type BackendShiftAssignmentResponse,
  type BackendShiftResponse,
  toAttendanceRecord,
  toDepartment,
  toEmployee,
  toAnnouncement,
  toDailyCommitment,
  toHourlyUpdate,
  toLeaderboardEntry,
  toLeaveBalance,
  toMonthlyTarget,
  toLeaveRequest,
  toOfficeLocation,
  toPayrollEntry,
  toPublicHoliday,
  toRegularization,
  toShift,
  toShiftAssignment,
} from '@/services/backendAdapters';
import { unwrapPagedResponse, unwrapResponse } from '@/services/contracts';
import { http } from '@/services/http';

interface LoginRequestPayload {
  username: string;
  password: string;
}

/**
 * Public self-service register endpoint. Only username / password / email
 * / employeeId are accepted — the backend (`AuthService.register`) ALWAYS
 * grants ROLE_EMPLOYEE and logs a warning if the caller attempts to send
 * a `roles` field (C-1 invariant). Do not add `roles` here — provisioning
 * privileged accounts MUST go through `api.admin.createUser`.
 */
interface RegisterRequestPayload {
  username: string;
  password: string;
  email: string;
  employeeId: string;
}

export interface PasswordChangePayload {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * Admin-only user provisioning payload. Mirrors
 * `com.financebuddha.finbud.hrms.dto.auth.AdminCreateUserRequest`.
 * If `password` is omitted the backend falls back to
 * `system_config.auth.default_password` (Finbud seed: `Welcome@123`).
 * `mustChangePassword` defaults to `true` on the backend when omitted.
 */
export interface AdminCreateUserPayload {
  employeeId: string;
  username?: string;
  email?: string;
  password?: string;
  roles?: string[];
  mustChangePassword?: boolean;
}

/**
 * Mirrors {@code com.financebuddha.finbud.hrms.dto.auth.UserAccountResponse}.
 * Role names are in the full {@code ROLE_XXX} form. {@code mustChangePassword}
 * is a server-derived convenience flag (equals {@code passwordChangedAt==null}).
 */
export interface BackendUserAccountResponse {
  userId: number;
  username: string;
  employeeRowId?: number | null;
  employeeId?: string | null;
  fullName?: string | null;
  email?: string | null;
  isActive: boolean;
  roles: string[];
  lastLoginAt?: string | null;
  passwordChangedAt?: string | null;
  mustChangePassword: boolean;
  locked: boolean;
  lockedUntil?: string | null;
  failedLoginAttempts?: number | null;
}

/**
 * Mirrors {@code PasswordResetResponse}. The {@code temporaryPassword} field
 * is the plaintext default password — surface it exactly once in a modal,
 * never log it, never keep it around.
 */
export interface BackendPasswordResetResponse {
  userId: number;
  username: string;
  temporaryPassword: string;
  mustChangePassword: boolean;
}

/**
 * Mirrors {@code com.financebuddha.finbud.hrms.dto.employee.EmployeeCreateResponse}.
 * Returned from {@code POST /api/employees}. The credential fields are
 * populated only on the happy path where auto-provisioning succeeded; the
 * UI must check {@code userProvisioned} before surfacing them.
 */
export interface BackendEmployeeCreateResponse {
  employee: BackendEmployeeResponse;
  userProvisioned: boolean;
  generatedUsername?: string | null;
  generatedTemporaryPassword?: string | null;
  provisioningSkippedReason?: string | null;
}

/**
 * UI-friendly adapter of the create response — the employee is converted
 * to the frontend {@link Employee} shape while the provisioning fields are
 * passed through as-is so the {@code EmployeesPage} modal can render them.
 */
export interface EmployeeCreateResult {
  employee: Employee;
  userProvisioned: boolean;
  generatedUsername?: string | null;
  generatedTemporaryPassword?: string | null;
  provisioningSkippedReason?: string | null;
}

export interface ImportEmployeesOptions {
  dryRun?: boolean;
  includeResigned?: boolean;
  createUsers?: boolean;
}

/**
 * Mirrors {@code com.financebuddha.finbud.hrms.dto.imports.ImportResponse}.
 * Canonical field names match the backend DTO exactly; legacy aliases
 * (`totalRows`, `errors`) are kept optional so older code paths that used
 * the previous shape still type-check while they are migrated.
 */
export interface BackendImportResponse {
  totalRecords?: number;
  insertedCount: number;
  updatedCount: number;
  skippedCount: number;
  failureCount: number;
  successCount?: number;
  dryRun?: boolean;
  includeResigned?: boolean;
  /**
   * Per-row audit trail. Each entry carries a `status` of
   * `IMPORTED | UPDATED | SKIPPED | FAILED`. Row-level errors are the
   * entries whose status is `FAILED` — surface those in the UI.
   */
  results?: Array<{
    rowNumber: number;
    employeeCode?: string;
    employeeName?: string;
    status: 'IMPORTED' | 'UPDATED' | 'SKIPPED' | 'FAILED' | string;
    message?: string;
  }>;
  warnings?: string[];
  // -----------------------------------------------------------------
  // Legacy aliases — older backend builds exposed these names.
  // Kept optional so the new UI gracefully falls back if the field
  // shape changes server-side.
  // -----------------------------------------------------------------
  /** @deprecated backend now exposes this as `totalRecords` */
  totalRows?: number;
  /** @deprecated backend now exposes failed rows inside `results[]` where `status === 'FAILED'` */
  errors?: Array<{ rowNumber: number; employeeId?: string; message: string }>;
}

export interface BackendImportDto {
  employeeId: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  personalEmail?: string;
  officialEmail?: string;
  designation?: string;
  departmentName?: string;
  managerName?: string;
  dateOfJoining?: string;
  status?: string;
  [key: string]: unknown;
}

/**
 * Mirrors com.financebuddha.finbud.hrms.dto.employee.EmployeeDetailResponse.
 * Only ADMIN / HR can fetch this shape — contains finance-sensitive data.
 */
export interface BackendEmployeeDetailResponse {
  employee: BackendEmployeeResponse;
  salaryInfo?: {
    salaryStructureId?: number | null;
    structureType?: string | null;
    monthlyGrossCtc?: number | null;
    nth?: number | null;
    annualCtc?: number | null;
    monthlyCtc?: number | null;
    employerPf?: number | null;
    employeePf?: number | null;
    employerEsi?: number | null;
    employeeEsi?: number | null;
    lwfAmount?: number | null;
    tdsAmount?: number | null;
    tdsRatePercent?: number | null;
    incentives?: number | null;
    otherDeductions?: number | null;
    numOfMonths?: number | null;
    effectiveFrom?: string | null;
    effectiveTo?: string | null;
    isActive?: boolean | null;
  };
  bankInfo?: {
    accountNumber?: string | null;
    ifscCode?: string | null;
    bankName?: string | null;
    accountType?: string | null;
    branch?: string | null;
    salaryPaymentMode?: string | null;
    ddPayableAt?: string | null;
    nameAsPerBank?: string | null;
    iban?: string | null;
  };
  identityInfo?: {
    panNumber?: string | null;
    aadhaarNumber?: string | null;
    aadhaarEnrolmentNo?: string | null;
    aadhaarName?: string | null;
    uanNumber?: string | null;
    pfNumber?: string | null;
    pfScheme?: string | null;
    pfJoiningDate?: string | null;
    esiNumber?: string | null;
    pfEligible?: boolean | null;
    esiEligible?: boolean | null;
    lwfEligible?: boolean | null;
    existingPfMember?: boolean | null;
    excessEpfEligible?: boolean | null;
    excessEpsEligible?: boolean | null;
  };
}

/**
 * Finbud V4 CTC / NTH salary model.
 * Mirrors com.financebuddha.finbud.hrms.dto.salary.SalaryStructureRequest.
 *
 * `structureType`, `monthlyGrossCtc`, `nth`, and `effectiveFrom` are required
 * on the backend. The legacy component fields (basicSalary, hra, …) are
 * accepted for back-compat with pre-V4 records but should not be sent for
 * new Finbud employees.
 */
export type SalaryStructureType = 'CONTRACT' | 'MANAGEMENT' | 'HIGHLY_SKILLED';

export interface BackendSalaryStructureRequest {
  // CTC model (required)
  structureType: SalaryStructureType;
  monthlyGrossCtc: number;
  nth: number;
  // Optional overrides on top of the structure type defaults
  tdsAmount?: number | null;
  tdsRatePercent?: number | null;
  employerPf?: number | null;
  employeePf?: number | null;
  employerEsi?: number | null;
  employeeEsi?: number | null;
  lwfAmount?: number | null;
  incentives?: number | null;
  otherDeductions?: number | null;
  numOfMonths?: number | null;
  annualCtc?: number | null;
  // Validity
  effectiveFrom: string;
  effectiveTo?: string | null;
  isActive?: boolean;
}

export interface BackendSalaryStructureResponse {
  id: number;
  employeeId: number;
  employeeName?: string | null;
  // CTC model
  structureType: SalaryStructureType | null;
  monthlyGrossCtc: number | null;
  nth: number | null;
  tdsAmount: number | null;
  tdsRatePercent: number | null;
  employerPf: number | null;
  employeePf: number | null;
  employerEsi: number | null;
  employeeEsi: number | null;
  lwfAmount: number | null;
  incentives: number | null;
  otherDeductions: number | null;
  numOfMonths: number | null;
  annualCtc: number | null;
  monthlyCtc: number | null;
  // Legacy view (null for new V4 records)
  basicSalary: number | null;
  hra: number | null;
  da?: number | null;
  conveyanceAllowance: number | null;
  medicalAllowance: number | null;
  specialAllowance: number | null;
  totalAllowances?: number | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  isActive: boolean;
  createdAt?: string | null;
}

export interface BackendPayrollSummaryResponse {
  month: number;
  year: number;
  totalEmployees: number;
  totalGrossEarnings: number;
  totalDeductions: number;
  totalNetPay: number;
  pendingCount?: number;
  approvedCount?: number;
  paidCount?: number;
}

/**
 * Mirrors {@code com.financebuddha.finbud.hrms.dto.dashboard.DashboardStatsResponse}.
 * Returned from {@code GET /api/dashboard/stats}. All long fields can be 0 — the
 * backend never returns null for the count buckets.
 */
export interface BackendDashboardDepartmentStat {
  departmentId: number;
  departmentName: string;
  employeeCount: number;
}

export interface BackendDashboardStatsResponse {
  // Employee
  totalEmployees: number;
  activeEmployees: number;
  onLeaveEmployees: number;
  newEmployeesThisMonth: number;
  // Attendance
  presentToday: number;
  absentToday: number;
  lateToday: number;
  onTimePercentage: number;
  // Leave
  pendingLeaves: number;
  approvedLeavesThisMonth: number;
  rejectedLeavesThisMonth: number;
  // Payroll (BigDecimal serialises as a number in JSON)
  monthlyPayroll: number;
  totalDeductionsThisMonth: number;
  paidPayrollsThisMonth: number;
  pendingPayrolls: number;
  // Departments
  totalDepartments: number;
  departmentStats: BackendDashboardDepartmentStat[];
}

/**
 * T2-2 (portal-first attendance): the old kiosk-only punch flow has been
 * retired. Every employee now marks themselves in/out from the authenticated
 * portal and the backend resolves the employee from the JWT
 * (`@CurrentUser UserPrincipal`). The legacy `X-Device-Api-Key` header is
 * no longer required — `DeviceApiKeyFilter` short-circuits to a no-op on the
 * server when the property is unset, and we intentionally stop reading
 * `VITE_DEVICE_API_KEY` here so a stale build-time value can't accidentally
 * hijack requests.
 */

/**
 * Portal punch payload. Mirrors
 * {@code com.financebuddha.finbud.hrms.dto.attendance.PunchRequest}. Every
 * field is optional — the server stamps the timestamp and resolves the
 * employee from the JWT. Geo coordinates are only required when the
 * employee's active office has {@code enforceGeofence=true}.
 */
export interface AttendancePunchPayload {
  latitude?: number;
  longitude?: number;
  /** GPS accuracy radius in metres — useful when the backend rejects on geofence. */
  accuracyMeters?: number;
  /** Short human label from the browser (e.g. "Near Andheri West"). */
  locationLabel?: string;
  deviceId?: string;
  notes?: string;
}

/** Mirrors {@code AttendanceManualEntryRequest}. */
export interface AttendanceManualEntryPayload {
  employeeId: number;
  /** ISO-8601 date (YYYY-MM-DD). */
  attendanceDate: string;
  /** ISO-8601 local date-time (or omit for status-only entries). */
  punchIn?: string;
  punchOut?: string;
  status:
    | 'PRESENT'
    | 'ABSENT'
    | 'HALF_DAY'
    | 'ON_LEAVE'
    | 'HOLIDAY'
    | 'WEEKLY_OFF'
    | 'PENDING'
    | 'AUTO_ABSENT'
    | 'MISSING_PUNCH';
  notes?: string;
}

/** Mirrors {@code OfficeLocationRequest}. */
export interface OfficeLocationPayload {
  name: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  geofenceRadiusMeters?: number;
  enforceGeofence?: boolean;
  isActive?: boolean;
}

/** Mirrors {@code PublicHolidayRequest}. */
export interface PublicHolidayPayload {
  /** ISO-8601 date (YYYY-MM-DD). */
  holidayDate: string;
  name: string;
  description?: string;
  isOptional?: boolean;
}

/** Mirrors {@code RegularizationRequestDto}. */
export interface RegularizationPayload {
  /** ISO-8601 date (YYYY-MM-DD). */
  attendanceDate: string;
  /** ISO-8601 local date-time. */
  requestedPunchIn?: string;
  requestedPunchOut?: string;
  reason: string;
}

/**
 * Shared 404 classifier. Backend services throw ResourceNotFoundException
 * which bubbles out as HTTP 404 — callers that want a "not found => null"
 * semantic wrap their `http.get` in try/catch and use this helper so the
 * check stays in one place.
 */
const isNotFoundError = (err: unknown): boolean =>
  (err as { response?: { status?: number } } | undefined)?.response?.status === 404;

export const api = {
  auth: {
    login: async (payload: LoginRequestPayload) =>
      unwrapResponse<BackendLoginResponse>(await http.post('/auth/login', payload)),
    logout: async () => {
      await unwrapResponse<void>(await http.post('/auth/logout'));
    },
    register: async (payload: RegisterRequestPayload) =>
      unwrapResponse<BackendLoginResponse>(await http.post('/auth/register', payload)),
    refresh: async (refreshToken: string) =>
      unwrapResponse<BackendLoginResponse>(await http.post('/auth/refresh-token', null, { params: { refreshToken } })),
    changePassword: async (payload: PasswordChangePayload) => {
      await unwrapResponse<void>(await http.post('/auth/change-password', payload));
    },
  },
  admin: {
    /**
     * Provision a User account for an existing Employee. ADMIN role
     * required. Returns the hydrated user descriptor but *no* tokens —
     * the provisioned user still has to log in themselves.
     */
    createUser: async (payload: AdminCreateUserPayload): Promise<BackendLoginResponse> =>
      unwrapResponse<BackendLoginResponse>(await http.post('/admin/users', payload)),
    importEmployees: async (
      file: File,
      options: ImportEmployeesOptions = {},
    ): Promise<BackendImportResponse> => {
      const form = new FormData();
      form.append('file', file);
      // The global axios client has a 15s timeout — fine for normal CRUD,
      // but the real import writes hundreds of employee + user rows inside
      // a single transaction and can legitimately take 30-60+ seconds. Give
      // the import endpoints 5 minutes so a slow-but-successful run doesn't
      // trigger a spurious client timeout while the server is still
      // committing. (Dry-runs are fast but share the same client.)
      const response = await http.post('/admin/import/employees', form, {
        timeout: 5 * 60 * 1000,
        params: {
          dryRun: options.dryRun ?? false,
          includeResigned: options.includeResigned ?? false,
          createUsers: options.createUsers ?? true,
        },
      });
      return unwrapResponse<BackendImportResponse>(response);
    },
    previewEmployees: async (file: File): Promise<BackendImportDto[]> => {
      const form = new FormData();
      form.append('file', file);
      const response = await http.post('/admin/import/employees/preview', form, {
        timeout: 2 * 60 * 1000,
      });
      return unwrapResponse<BackendImportDto[]>(response);
    },
    cleanupEmployees: async (preserveSystemUsers = true) => {
      await unwrapResponse(
        await http.delete('/admin/import/employees/cleanup', {
          timeout: 2 * 60 * 1000,
          params: { preserveSystemUsers },
        }),
      );
    },
    // ----------------------------------------------------------------
    // User account management (Admin + HR). The server enforces that HR
    // cannot grant/revoke ADMIN or HR roles, cannot toggle the status of
    // Admin/HR accounts, and cannot reset the password of Admin/HR
    // accounts — see AdminUserServiceImpl.
    // ----------------------------------------------------------------
    /**
     * Look up the login account attached to an employee code (e.g. ND33454,
     * FBD260005). Returns null when the employee exists but has no login
     * provisioned yet (the UI uses this to show a "Create login" button
     * instead of the role editor).
     */
    getUserByEmployeeCode: async (employeeCode: string): Promise<BackendUserAccountResponse | null> => {
      try {
        return unwrapResponse<BackendUserAccountResponse>(
          await http.get(`/admin/users/by-employee/${employeeCode}`),
        );
      } catch (err) {
        // ResourceNotFoundException surfaces as HTTP 404. Treat it as the
        // "no login yet" case rather than an error.
        const status = (err as { response?: { status?: number } }).response?.status;
        if (status === 404) return null;
        throw err;
      }
    },
    updateUserRoles: async (
      userId: number,
      roles: string[],
    ): Promise<BackendUserAccountResponse> =>
      unwrapResponse<BackendUserAccountResponse>(
        await http.patch(`/admin/users/${userId}/roles`, { roles }),
      ),
    updateUserStatus: async (
      userId: number,
      isActive: boolean,
    ): Promise<BackendUserAccountResponse> =>
      unwrapResponse<BackendUserAccountResponse>(
        await http.patch(`/admin/users/${userId}/status`, { isActive }),
      ),
    resetUserPassword: async (userId: number): Promise<BackendPasswordResetResponse> =>
      unwrapResponse<BackendPasswordResetResponse>(
        await http.patch(`/admin/users/${userId}/password/reset`),
      ),
  },
  employees: {
    /**
     * Load the full directory in one request. The backend paginates by
     * default (page=0, size=20), so without these params the UI would
     * silently show only the first 20 rows. Until a proper paginated /
     * virtualised table is built we pass an explicit large page and sort
     * by {@code employeeId} so the list renders in a predictable order.
     */
    list: async (): Promise<Employee[]> => {
      const response = unwrapPagedResponse<BackendEmployeeResponse>(
        await http.get('/employees', {
          params: {
            page: 0,
            size: 2000,
            sortBy: 'employeeId',
            sortDirection: 'asc',
          },
        }),
      );
      return response.items.map(toEmployee);
    },
    getById: async (id: string | number): Promise<Employee | null> => {
      const response = unwrapResponse<BackendEmployeeResponse>(await http.get(`/employees/${id}`));
      return toEmployee(response);
    },
    getByEmployeeId: async (employeeId: string): Promise<Employee | null> => {
      const response = unwrapResponse<BackendEmployeeResponse>(await http.get(`/employees/employee-id/${employeeId}`));
      return toEmployee(response);
    },
    getRawByEmployeeId: async (employeeId: string): Promise<BackendEmployeeResponse> =>
      unwrapResponse<BackendEmployeeResponse>(await http.get(`/employees/employee-id/${employeeId}`)),
    getDetails: async (id: number): Promise<BackendEmployeeDetailResponse> =>
      unwrapResponse<BackendEmployeeDetailResponse>(await http.get(`/employees/${id}/details`)),
    listByManager: async (managerId: number): Promise<Employee[]> => {
      const response = unwrapPagedResponse<BackendEmployeeResponse>(
        await http.get(`/employees/manager/${managerId}`, {
          params: {
            page: 0,
            size: 2000,
            sortBy: 'employeeId',
            sortDirection: 'asc',
          },
        }),
      );
      return response.items.map(toEmployee);
    },
    /**
     * Create an employee. The backend now auto-provisions a login account
     * in the same transaction, so the response carries both the new
     * Employee and the generated credentials (username + one-time plaintext
     * temp password). The UI must surface the credentials exactly once —
     * the hashed form is what's stored server-side, the plaintext is a
     * one-time leak.
     */
    create: async (payload: Record<string, unknown>): Promise<EmployeeCreateResult> => {
      const response = unwrapResponse<BackendEmployeeCreateResponse>(
        await http.post('/employees', payload),
      );
      return {
        employee: toEmployee(response.employee),
        userProvisioned: response.userProvisioned,
        generatedUsername: response.generatedUsername,
        generatedTemporaryPassword: response.generatedTemporaryPassword,
        provisioningSkippedReason: response.provisioningSkippedReason,
      };
    },
    update: async (id: number, payload: Record<string, unknown>): Promise<Employee> => {
      const response = unwrapResponse<BackendEmployeeResponse>(await http.put(`/employees/${id}`, payload));
      return toEmployee(response);
    },
    remove: async (id: number) => {
      await unwrapResponse(await http.delete(`/employees/${id}`));
    },
    /**
     * Upload (or replace) an employee's profile picture. Authorization is
     * enforced server-side: the caller must be ADMIN / HR, or be the
     * employee themselves (principal.employeeId == employeeCode).
     *
     * Returns the freshly-mapped Employee — response.profilePicture is a
     * short-lived presigned S3 URL suitable for <img src="...">.
     */
    uploadAvatar: async (employeeCode: string, file: File): Promise<Employee> => {
      const form = new FormData();
      form.append('file', file);
      const response = await http.post(`/employees/${employeeCode}/avatar`, form);
      return toEmployee(unwrapResponse<BackendEmployeeResponse>(response));
    },
    /**
     * Remove the current profile picture. Idempotent — safe to call even
     * if the employee has no picture.
     */
    removeAvatar: async (employeeCode: string): Promise<Employee> => {
      const response = await http.delete(`/employees/${employeeCode}/avatar`);
      return toEmployee(unwrapResponse<BackendEmployeeResponse>(response));
    },
  },
  attendance: {
    /**
     * Portal punch payload. All fields are optional — the server stamps the
     * timestamp and the employee id. Geo coordinates are required only when
     * the employee's office has `enforceGeofence=true`; the back-end returns
     * a 4xx with a clear message otherwise, which the UI surfaces verbatim.
     */
    punchIn: async (payload: AttendancePunchPayload = {}): Promise<AttendanceRecord> => {
      const response = unwrapResponse<BackendAttendanceResponse>(
        await http.post('/attendance/punch-in', payload),
      );
      return toAttendanceRecord(response);
    },
    punchOut: async (payload: AttendancePunchPayload = {}): Promise<AttendanceRecord> => {
      const response = unwrapResponse<BackendAttendanceResponse>(
        await http.post('/attendance/punch-out', payload),
      );
      return toAttendanceRecord(response);
    },

    // -------- Approval queue (TL direct-reports, HR, Admin) ---------
    pendingApprovals: async (): Promise<AttendanceRecord[]> => {
      const response = unwrapResponse<BackendAttendanceResponse[]>(
        await http.get('/attendance/approvals/pending'),
      );
      return response.map(toAttendanceRecord);
    },
    review: async (
      attendanceId: number,
      payload: { approve: boolean; rejectionReason?: string },
    ): Promise<AttendanceRecord> => {
      const response = unwrapResponse<BackendAttendanceResponse>(
        await http.post(`/attendance/${attendanceId}/review`, payload),
      );
      return toAttendanceRecord(response);
    },

    // -------- HR / Admin manual entry -------------------------------
    manualEntry: async (payload: AttendanceManualEntryPayload): Promise<AttendanceRecord> => {
      const response = unwrapResponse<BackendAttendanceResponse>(
        await http.post('/attendance/manual-entry', payload),
      );
      return toAttendanceRecord(response);
    },

    // -------- Read / query APIs -------------------------------------
    listByEmployeeIds: async (employeeIds: number[]): Promise<AttendanceRecord[]> => {
      const attendance = await Promise.all(employeeIds.map((employeeId) => api.attendance.listForEmployee(employeeId)));
      return attendance.flat();
    },
    listForEmployee: async (employeeId: number): Promise<AttendanceRecord[]> => {
      const response = unwrapPagedResponse<BackendAttendanceResponse>(await http.get(`/attendance/employee/${employeeId}`));
      return response.items.map(toAttendanceRecord);
    },
    listForEmployeeInRange: async (
      employeeId: number,
      startDate: string,
      endDate: string,
    ): Promise<AttendanceRecord[]> => {
      const response = unwrapResponse<BackendAttendanceResponse[]>(
        await http.get(`/attendance/employee/${employeeId}/range`, { params: { startDate, endDate } }),
      );
      return response.map(toAttendanceRecord);
    },
    getByEmployeeAndDate: async (employeeId: number, date: string): Promise<AttendanceRecord | null> => {
      try {
        const response = unwrapResponse<BackendAttendanceResponse>(
          await http.get(`/attendance/employee/${employeeId}/date/${date}`),
        );
        return toAttendanceRecord(response);
      } catch (error) {
        if (isNotFoundError(error)) return null;
        throw error;
      }
    },
    lateComers: async (date: string): Promise<AttendanceRecord[]> => {
      const response = unwrapResponse<BackendAttendanceResponse[]>(await http.get(`/attendance/late-comers/${date}`));
      return response.map(toAttendanceRecord);
    },
    absentByDate: async (date: string): Promise<AttendanceRecord[]> => {
      const response = unwrapResponse<BackendAttendanceResponse[]>(await http.get(`/attendance/absent/${date}`));
      return response.map(toAttendanceRecord);
    },
    overtimeRange: async (startDate: string, endDate: string): Promise<AttendanceRecord[]> => {
      const response = unwrapResponse<BackendAttendanceResponse[]>(
        await http.get('/attendance/overtime', { params: { startDate, endDate } }),
      );
      return response.map(toAttendanceRecord);
    },
    getSummary: async (employeeId: number, month: number, year: number) =>
      unwrapResponse(await http.get(`/attendance/employee/${employeeId}/summary`, { params: { month, year } })),
    hasPunchedInToday: async (employeeId: number): Promise<boolean> =>
      unwrapResponse<boolean>(await http.get(`/attendance/employee/${employeeId}/punched-in-today`)),
    hasPunchedOutToday: async (employeeId: number): Promise<boolean> =>
      unwrapResponse<boolean>(await http.get(`/attendance/employee/${employeeId}/punched-out-today`)),
  },
  officeLocations: {
    /** HR/Admin list — includes inactive rows for the admin page. */
    list: async (): Promise<OfficeLocation[]> => {
      const response = unwrapResponse<BackendOfficeLocationResponse[]>(await http.get('/office-locations'));
      return response.map(toOfficeLocation);
    },
    /** Active-only — safe for the MarkAttendanceCard to surface to every employee. */
    listActive: async (): Promise<OfficeLocation[]> => {
      const response = unwrapResponse<BackendOfficeLocationResponse[]>(
        await http.get('/office-locations/active'),
      );
      return response.map(toOfficeLocation);
    },
    getById: async (id: number): Promise<OfficeLocation> => {
      const response = unwrapResponse<BackendOfficeLocationResponse>(await http.get(`/office-locations/${id}`));
      return toOfficeLocation(response);
    },
    create: async (payload: OfficeLocationPayload): Promise<OfficeLocation> => {
      const response = unwrapResponse<BackendOfficeLocationResponse>(await http.post('/office-locations', payload));
      return toOfficeLocation(response);
    },
    update: async (id: number, payload: OfficeLocationPayload): Promise<OfficeLocation> => {
      const response = unwrapResponse<BackendOfficeLocationResponse>(await http.put(`/office-locations/${id}`, payload));
      return toOfficeLocation(response);
    },
    remove: async (id: number): Promise<void> => {
      await unwrapResponse(await http.delete(`/office-locations/${id}`));
    },
  },
  publicHolidays: {
    list: async (): Promise<PublicHoliday[]> => {
      const response = unwrapResponse<BackendPublicHolidayResponse[]>(await http.get('/public-holidays'));
      return response.map(toPublicHoliday);
    },
    listByYear: async (year: number): Promise<PublicHoliday[]> => {
      const response = unwrapResponse<BackendPublicHolidayResponse[]>(
        await http.get(`/public-holidays/year/${year}`),
      );
      return response.map(toPublicHoliday);
    },
    listByRange: async (startDate: string, endDate: string): Promise<PublicHoliday[]> => {
      const response = unwrapResponse<BackendPublicHolidayResponse[]>(
        await http.get('/public-holidays/range', { params: { startDate, endDate } }),
      );
      return response.map(toPublicHoliday);
    },
    getById: async (id: number): Promise<PublicHoliday> => {
      const response = unwrapResponse<BackendPublicHolidayResponse>(await http.get(`/public-holidays/${id}`));
      return toPublicHoliday(response);
    },
    create: async (payload: PublicHolidayPayload): Promise<PublicHoliday> => {
      const response = unwrapResponse<BackendPublicHolidayResponse>(await http.post('/public-holidays', payload));
      return toPublicHoliday(response);
    },
    update: async (id: number, payload: PublicHolidayPayload): Promise<PublicHoliday> => {
      const response = unwrapResponse<BackendPublicHolidayResponse>(await http.put(`/public-holidays/${id}`, payload));
      return toPublicHoliday(response);
    },
    remove: async (id: number): Promise<void> => {
      await unwrapResponse(await http.delete(`/public-holidays/${id}`));
    },
  },
  regularizations: {
    submit: async (payload: RegularizationPayload): Promise<Regularization> => {
      const response = unwrapResponse<BackendRegularizationResponse>(await http.post('/regularizations', payload));
      return toRegularization(response);
    },
    cancelOwn: async (id: number): Promise<void> => {
      await unwrapResponse(await http.delete(`/regularizations/${id}`));
    },
    review: async (
      id: number,
      payload: { approve: boolean; reviewNotes?: string },
    ): Promise<Regularization> => {
      const response = unwrapResponse<BackendRegularizationResponse>(
        await http.post(`/regularizations/${id}/review`, payload),
      );
      return toRegularization(response);
    },
    listMine: async (): Promise<Regularization[]> => {
      const response = unwrapResponse<BackendRegularizationResponse[]>(await http.get('/regularizations/me'));
      return response.map(toRegularization);
    },
    listPending: async (): Promise<Regularization[]> => {
      const response = unwrapResponse<BackendRegularizationResponse[]>(await http.get('/regularizations/pending'));
      return response.map(toRegularization);
    },
    getById: async (id: number): Promise<Regularization> => {
      const response = unwrapResponse<BackendRegularizationResponse>(await http.get(`/regularizations/${id}`));
      return toRegularization(response);
    },
  },
  shifts: {
    list: async (): Promise<Shift[]> => {
      const response = unwrapResponse<BackendShiftResponse[]>(await http.get('/shifts'));
      return response.map(toShift);
    },
    getById: async (id: number): Promise<Shift> => {
      const response = unwrapResponse<BackendShiftResponse>(await http.get(`/shifts/${id}`));
      return toShift(response);
    },
    create: async (payload: Record<string, unknown>): Promise<Shift> => {
      const response = unwrapResponse<BackendShiftResponse>(await http.post('/shifts', payload));
      return toShift(response);
    },
    update: async (id: number, payload: Record<string, unknown>): Promise<Shift> => {
      const response = unwrapResponse<BackendShiftResponse>(await http.put(`/shifts/${id}`, payload));
      return toShift(response);
    },
    remove: async (id: number): Promise<void> => {
      unwrapResponse(await http.delete(`/shifts/${id}`));
    },
    /**
     * Shift assignment APIs. Backed by `/api/shifts/employees/{id}/assignments`
     * and `/api/shifts/assignments/{id}` on the backend.
     */
    assignments: {
      listForEmployee: async (employeeId: number): Promise<ShiftAssignment[]> => {
        const response = unwrapResponse<BackendShiftAssignmentResponse[]>(
          await http.get(`/shifts/employees/${employeeId}/assignments`),
        );
        return response.map(toShiftAssignment);
      },
      getCurrentForEmployee: async (employeeId: number): Promise<ShiftAssignment | null> => {
        const response = unwrapResponse<BackendShiftAssignmentResponse | null>(
          await http.get(`/shifts/employees/${employeeId}/assignments/current`),
        );
        return response ? toShiftAssignment(response) : null;
      },
      listForShiftType: async (shiftTypeId: number): Promise<ShiftAssignment[]> => {
        const response = unwrapResponse<BackendShiftAssignmentResponse[]>(
          await http.get(`/shifts/${shiftTypeId}/assignments`),
        );
        return response.map(toShiftAssignment);
      },
      getById: async (id: number): Promise<ShiftAssignment> => {
        const response = unwrapResponse<BackendShiftAssignmentResponse>(
          await http.get(`/shifts/assignments/${id}`),
        );
        return toShiftAssignment(response);
      },
      create: async (
        employeeId: number,
        payload: { shiftTypeId: number; effectiveFrom: string; effectiveTo: string | null },
      ): Promise<ShiftAssignment> => {
        const response = unwrapResponse<BackendShiftAssignmentResponse>(
          await http.post(`/shifts/employees/${employeeId}/assignments`, payload),
        );
        return toShiftAssignment(response);
      },
      update: async (
        id: number,
        payload: { shiftTypeId: number; effectiveFrom: string; effectiveTo: string | null },
      ): Promise<ShiftAssignment> => {
        const response = unwrapResponse<BackendShiftAssignmentResponse>(
          await http.put(`/shifts/assignments/${id}`, payload),
        );
        return toShiftAssignment(response);
      },
      remove: async (id: number): Promise<void> => {
        unwrapResponse(await http.delete(`/shifts/assignments/${id}`));
      },
    },
  },
  leave: {
    listByEmployeeIds: async (employeeIds: number[]): Promise<LeaveRequest[]> => {
      const leaves = await Promise.all(employeeIds.map((employeeId) => api.leave.listForEmployee(employeeId)));
      return leaves.flat();
    },
    listForEmployee: async (employeeId: number): Promise<LeaveRequest[]> => {
      const response = unwrapPagedResponse<BackendLeaveResponse>(await http.get(`/leaves/employee/${employeeId}`));
      return response.items.map(toLeaveRequest);
    },
    listForManager: async (managerId: number): Promise<LeaveRequest[]> => {
      const response = unwrapPagedResponse<BackendLeaveResponse>(await http.get(`/leaves/manager/${managerId}`));
      return response.items.map(toLeaveRequest);
    },
    apply: async (payload: Record<string, unknown>): Promise<LeaveRequest> => {
      const response = unwrapResponse<BackendLeaveResponse>(await http.post('/leaves/apply', payload));
      return toLeaveRequest(response);
    },
    approve: async (leaveRequestId: number, payload: Record<string, unknown>): Promise<LeaveRequest> => {
      const response = unwrapResponse<BackendLeaveResponse>(await http.post(`/leaves/${leaveRequestId}/approve`, payload));
      return toLeaveRequest(response);
    },
    reject: async (leaveRequestId: number, reason: string): Promise<LeaveRequest> => {
      const response = unwrapResponse<BackendLeaveResponse>(
        await http.post(`/leaves/${leaveRequestId}/reject`, null, { params: { reason } }),
      );
      return toLeaveRequest(response);
    },
    /**
     * Per-employee, per-calendar-year balance. Returns the V12 combined-pool
     * shape — casual+sick share one bucket, paid has its own.
     */
    getLeaveBalance: async (employeeId: number, year: number): Promise<LeaveBalance> => {
      const response = unwrapResponse<BackendLeaveBalanceResponse>(
        await http.get(`/leaves/balance/${employeeId}`, { params: { year } }),
      );
      return toLeaveBalance(response);
    },
  },
  payroll: {
    listForEmployee: async (employeeId: number): Promise<PayrollEntry[]> => {
      const response = unwrapPagedResponse<BackendPayrollResponse>(await http.get(`/payroll/employee/${employeeId}`));
      return response.items.map(toPayrollEntry);
    },
    listByMonth: async (month: number, year: number): Promise<PayrollEntry[]> => {
      const response = unwrapPagedResponse<BackendPayrollResponse>(await http.get(`/payroll/month/${month}/year/${year}`));
      return response.items.map(toPayrollEntry);
    },
    generate: async (
      payload: {
        employeeId: number;
        month: number;
        year: number;
        /** When present, overrides attendance-based LOP for this run. */
        lopDays?: number;
        /** One-off incentive, overrides the structure's standing incentives. */
        incentivesOverride?: number;
        /** Positive or negative adjustment applied to net pay. */
        adjustments?: number;
        /** Shown on the payslip whenever adjustments is non-zero. */
        adjustmentReason?: string;
      },
    ): Promise<PayrollEntry> => {
      const response = unwrapResponse<BackendPayrollResponse>(
        await http.post('/payroll/generate', payload),
      );
      return toPayrollEntry(response);
    },
    generateAll: async (payload: { month: number; year: number }): Promise<PayrollEntry[]> => {
      const response = unwrapResponse<BackendPayrollResponse[]>(await http.post('/payroll/generate-all', payload));
      return response.map(toPayrollEntry);
    },
    approve: async (payrollId: number): Promise<PayrollEntry> => {
      const response = unwrapResponse<BackendPayrollResponse>(await http.post(`/payroll/${payrollId}/approve`));
      return toPayrollEntry(response);
    },
    markPaid: async (payrollId: number): Promise<PayrollEntry> => {
      const response = unwrapResponse<BackendPayrollResponse>(await http.post(`/payroll/${payrollId}/mark-paid`));
      return toPayrollEntry(response);
    },
    summary: async (month: number, year: number): Promise<BackendPayrollSummaryResponse> =>
      unwrapResponse<BackendPayrollSummaryResponse>(await http.get('/payroll/summary', { params: { month, year } })),
    payslipPdf: async (payrollId: number): Promise<Blob> => {
      // Backend now streams the payslip as application/pdf directly (see
      // PayrollController#generatePayslipPdf). We request it as an arraybuffer
      // so axios doesn't try to JSON-parse the bytes.
      const response = await http.get(`/payroll/${payrollId}/payslip`, {
        responseType: 'arraybuffer',
        headers: { Accept: 'application/pdf' },
      });
      return new Blob([response.data as ArrayBuffer], { type: 'application/pdf' });
    },
  },
  commitments: {
    daily: {
      /** Employee creates today's (or any) commitment with targets. Status DRAFT. */
      create: async (payload: {
        workDate: string;
        targetCalls: number;
        targetOtps: number;
        targetInterestedCustomers: number;
        targetDisbursalAmount: number;
        notes?: string;
      }): Promise<DailyCommitment> => {
        const response = unwrapResponse<BackendDailyCommitmentResponse>(
          await http.post('/commitments/daily', payload),
        );
        return toDailyCommitment(response);
      },
      /** Patch targets / actuals / notes on an existing DRAFT or REJECTED row. */
      update: async (
        commitmentId: number,
        payload: Partial<{
          targetCalls: number;
          targetOtps: number;
          targetInterestedCustomers: number;
          targetDisbursalAmount: number;
          actualCalls: number;
          actualOtps: number;
          actualInterestedCustomers: number;
          actualDisbursalAmount: number;
          notes: string;
        }>,
      ): Promise<DailyCommitment> => {
        const response = unwrapResponse<BackendDailyCommitmentResponse>(
          await http.put(`/commitments/daily/${commitmentId}`, payload),
        );
        return toDailyCommitment(response);
      },
      /** Submit for TL approval. */
      submit: async (commitmentId: number): Promise<DailyCommitment> => {
        const response = unwrapResponse<BackendDailyCommitmentResponse>(
          await http.post(`/commitments/daily/${commitmentId}/submit`),
        );
        return toDailyCommitment(response);
      },
      /** TL/HR/Admin approves or rejects. */
      review: async (
        commitmentId: number,
        payload: { approve: boolean; rejectionReason?: string },
      ): Promise<DailyCommitment> => {
        const response = unwrapResponse<BackendDailyCommitmentResponse>(
          await http.post(`/commitments/daily/${commitmentId}/review`, payload),
        );
        return toDailyCommitment(response);
      },
      /**
       * My row for a specific date. Returns null on 404 so the UI can
       * render the "no commitment yet — create one" state cleanly.
       */
      getMineForDate: async (date: string): Promise<DailyCommitment | null> => {
        try {
          const response = unwrapResponse<BackendDailyCommitmentResponse>(
            await http.get('/commitments/daily/me', { params: { date } }),
          );
          return toDailyCommitment(response);
        } catch (err) {
          if (isNotFoundError(err)) return null;
          throw err;
        }
      },
      /** My commitment history within a date window, newest first. */
      listMineForRange: async (startDate: string, endDate: string): Promise<DailyCommitment[]> => {
        const response = unwrapResponse<BackendDailyCommitmentResponse[]>(
          await http.get('/commitments/daily/me/range', { params: { startDate, endDate } }),
        );
        return response.map(toDailyCommitment);
      },
      /** Any employee's history (TL/HR/Admin). */
      listForEmployee: async (
        employeeId: number,
        startDate: string,
        endDate: string,
      ): Promise<DailyCommitment[]> => {
        const response = unwrapResponse<BackendDailyCommitmentResponse[]>(
          await http.get(`/commitments/daily/employee/${employeeId}/range`, { params: { startDate, endDate } }),
        );
        return response.map(toDailyCommitment);
      },
      /** TL pending-approval queue. */
      listPendingForManager: async (managerId: number): Promise<DailyCommitment[]> => {
        const response = unwrapResponse<BackendDailyCommitmentResponse[]>(
          await http.get(`/commitments/daily/manager/${managerId}/pending`),
        );
        return response.map(toDailyCommitment);
      },
      /** TL team snapshot for a single date. */
      listTeamForDate: async (managerId: number, date: string): Promise<DailyCommitment[]> => {
        const response = unwrapResponse<BackendDailyCommitmentResponse[]>(
          await http.get(`/commitments/daily/manager/${managerId}/team`, { params: { date } }),
        );
        return response.map(toDailyCommitment);
      },
    },
    hourly: {
      /** Upsert an hourly entry — same (date, slot) overwrites in place. */
      upsert: async (payload: {
        workDate: string;
        hourSlot: string;
        callsDone: number;
        otpsAchieved: number;
        interestedCustomers: number;
        notes?: string;
      }): Promise<HourlyUpdate> => {
        const response = unwrapResponse<BackendHourlyUpdateResponse>(
          await http.post('/commitments/hourly', payload),
        );
        return toHourlyUpdate(response);
      },
      remove: async (updateId: number): Promise<void> => {
        await unwrapResponse(await http.delete(`/commitments/hourly/${updateId}`));
      },
      listMineForDate: async (date: string): Promise<HourlyUpdate[]> => {
        const response = unwrapResponse<BackendHourlyUpdateResponse[]>(
          await http.get('/commitments/hourly/me', { params: { date } }),
        );
        return response.map(toHourlyUpdate);
      },
      listMineForRange: async (startDate: string, endDate: string): Promise<HourlyUpdate[]> => {
        const response = unwrapResponse<BackendHourlyUpdateResponse[]>(
          await http.get('/commitments/hourly/me/range', { params: { startDate, endDate } }),
        );
        return response.map(toHourlyUpdate);
      },
      listTeamForDate: async (managerId: number, date: string): Promise<HourlyUpdate[]> => {
        const response = unwrapResponse<BackendHourlyUpdateResponse[]>(
          await http.get(`/commitments/hourly/manager/${managerId}/team`, { params: { date } }),
        );
        return response.map(toHourlyUpdate);
      },
    },
    monthlyTargets: {
      /** Set / update an employee's monthly target. TL/HR/Admin only on the server. */
      upsert: async (
        employeeId: number,
        payload: {
          year: number;
          month: number;
          targetDisbursalAmount: number;
          targetLogins: number;
          notes?: string;
        },
      ): Promise<MonthlyTarget> => {
        const response = unwrapResponse<BackendMonthlyTargetResponse>(
          await http.put(`/commitments/monthly-target/${employeeId}`, payload),
        );
        return toMonthlyTarget(response);
      },
      /** My target with achieved overlay — returns a zero-target placeholder if not yet set. */
      getMine: async (year: number, month: number): Promise<MonthlyTarget> => {
        const response = unwrapResponse<BackendMonthlyTargetResponse>(
          await http.get('/commitments/monthly-target/me', { params: { year, month } }),
        );
        return toMonthlyTarget(response);
      },
      /** Read for any employee (TL/HR/Admin only). */
      getForEmployee: async (employeeId: number, year: number, month: number): Promise<MonthlyTarget> => {
        const response = unwrapResponse<BackendMonthlyTargetResponse>(
          await http.get(`/commitments/monthly-target/${employeeId}`, { params: { year, month } }),
        );
        return toMonthlyTarget(response);
      },
      /** A TL's team list for a period — direct reports + achieved overlay. */
      listForManager: async (managerId: number, year: number, month: number): Promise<MonthlyTarget[]> => {
        const response = unwrapResponse<BackendMonthlyTargetResponse[]>(
          await http.get(`/commitments/monthly-target/manager/${managerId}`, { params: { year, month } }),
        );
        return response.map(toMonthlyTarget);
      },
    },
  },
  leaderboard: {
    /**
     * Monthly disbursal leaderboard (Q3). Year + month default to the
     * current period if omitted on the wire. Any authenticated user reads.
     */
    monthlyDisbursal: async (params?: { year?: number; month?: number }): Promise<LeaderboardEntry[]> => {
      const response = unwrapResponse<BackendLeaderboardEntryResponse[]>(
        await http.get('/leaderboard/disbursal', { params }),
      );
      return response.map(toLeaderboardEntry);
    },
  },
  commitmentReports: {
    /** Stream the per-employee daily commitment Excel for the given window. */
    employeeXlsx: async (
      employeeId: number,
      startDate: string,
      endDate: string,
    ): Promise<Blob> => {
      const response = await http.get(`/reports/commitment/employee/${employeeId}.xlsx`, {
        params: { startDate, endDate },
        responseType: 'arraybuffer',
        headers: {
          Accept:
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        },
      });
      return new Blob([response.data as ArrayBuffer], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
    },
    /** Stream the per-team daily commitment Excel — TL/HR/Admin only on the server. */
    teamXlsx: async (
      managerId: number,
      startDate: string,
      endDate: string,
    ): Promise<Blob> => {
      const response = await http.get(`/reports/commitment/team/${managerId}.xlsx`, {
        params: { startDate, endDate },
        responseType: 'arraybuffer',
        headers: {
          Accept:
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        },
      });
      return new Blob([response.data as ArrayBuffer], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
    },
  },
  announcements: {
    /**
     * Active announcements newest-first — what every dashboard reads. The
     * backend filters to is_active=true, so archived rows never reach
     * the frontend through this method.
     */
    listActive: async (): Promise<Announcement[]> => {
      const response = unwrapResponse<BackendAnnouncementResponse[]>(await http.get('/announcements'));
      return response.map(toAnnouncement);
    },
    create: async (payload: {
      title: string;
      message: string;
      priority: 'HIGH' | 'MEDIUM' | 'LOW';
    }): Promise<Announcement> => {
      const response = unwrapResponse<BackendAnnouncementResponse>(
        await http.post('/announcements', payload),
      );
      return toAnnouncement(response);
    },
    update: async (
      id: number,
      payload: Partial<{
        title: string;
        message: string;
        priority: 'HIGH' | 'MEDIUM' | 'LOW';
        isActive: boolean;
      }>,
    ): Promise<Announcement> => {
      const response = unwrapResponse<BackendAnnouncementResponse>(
        await http.put(`/announcements/${id}`, payload),
      );
      return toAnnouncement(response);
    },
    /** Soft-delete via is_active=false. Backend is idempotent. */
    deactivate: async (id: number): Promise<void> => {
      await unwrapResponse(await http.delete(`/announcements/${id}`));
    },
  },
  systemConfig: {
    /**
     * Org-wide monthly disbursal goal. Readable by every authenticated
     * user — the dashboard tile shows the same value to everyone.
     * Returned as a plain number so the UI can run currency formatters
     * without parsing strings.
     */
    getOrgMonthlyGoal: async (): Promise<{ amount: number; currency: string }> => {
      const response = unwrapResponse<{ amount: number | string; currency: string }>(
        await http.get('/system-config/org-monthly-goal'),
      );
      return {
        amount: typeof response.amount === 'string' ? Number(response.amount) : (response.amount ?? 0),
        currency: response.currency ?? 'INR',
      };
    },
    /** Set / update the org-wide monthly disbursal goal. Admin-only on the server. */
    setOrgMonthlyGoal: async (amount: number): Promise<{ amount: number; currency: string }> => {
      const response = unwrapResponse<{ amount: number | string; currency: string }>(
        await http.put('/system-config/org-monthly-goal', { amount }),
      );
      return {
        amount: typeof response.amount === 'string' ? Number(response.amount) : (response.amount ?? 0),
        currency: response.currency ?? 'INR',
      };
    },
  },
  salary: {
    get: async (employeeId: number): Promise<BackendSalaryStructureResponse> =>
      unwrapResponse<BackendSalaryStructureResponse>(await http.get(`/salary/${employeeId}`)),
    create: async (employeeId: number, payload: BackendSalaryStructureRequest): Promise<BackendSalaryStructureResponse> =>
      unwrapResponse<BackendSalaryStructureResponse>(await http.post(`/salary/${employeeId}`, payload)),
    update: async (id: number, payload: BackendSalaryStructureRequest): Promise<BackendSalaryStructureResponse> =>
      unwrapResponse<BackendSalaryStructureResponse>(await http.put(`/salary/${id}`, payload)),
    deactivate: async (id: number) => {
      await unwrapResponse(await http.delete(`/salary/${id}`));
    },
    list: async (): Promise<BackendSalaryStructureResponse[]> =>
      unwrapResponse<BackendSalaryStructureResponse[]>(await http.get('/salary')),
  },
  departments: {
    list: async (): Promise<Department[]> => {
      const response = unwrapPagedResponse<BackendDepartmentResponse>(await http.get('/departments'));
      return response.items.map(toDepartment);
    },
    create: async (payload: Record<string, unknown>): Promise<Department> => {
      const response = unwrapResponse<BackendDepartmentResponse>(await http.post('/departments', payload));
      return toDepartment(response);
    },
    /**
     * Edit an existing department's name / code / description / manager.
     * Backend route: PUT /departments/{id} — gated to ADMIN/HR.
     *
     * Note: the backend's DepartmentRequest accepts a managerId field, but
     * for changing only the team leader prefer the dedicated assignManager
     * endpoint below — it has clearer audit semantics.
     */
    update: async (id: number, payload: Record<string, unknown>): Promise<Department> => {
      const response = unwrapResponse<BackendDepartmentResponse>(await http.put(`/departments/${id}`, payload));
      return toDepartment(response);
    },
    /**
     * Reassign the team leader of a department.
     * Backend route: POST /departments/{departmentId}/manager/{managerId}.
     * The backend re-derives the response so the new managerName comes
     * back in the same payload — no extra fetch needed.
     */
    assignManager: async (departmentId: number, managerId: number): Promise<Department> => {
      const response = unwrapResponse<BackendDepartmentResponse>(
        await http.post(`/departments/${departmentId}/manager/${managerId}`),
      );
      return toDepartment(response);
    },
    /**
     * Hard-delete a department.
     * Backend route: DELETE /departments/{id}. Returns 200 with `data: null`.
     * Will fail with a 4xx if the department still has employees — surface
     * that error to the caller so the UI can show "Move employees first".
     */
    remove: async (id: number): Promise<void> => {
      unwrapResponse<null>(await http.delete(`/departments/${id}`));
    },
  },
  dashboard: {
    /**
     * Aggregate KPIs for the home dashboards. Backend route is gated to
     * ADMIN / HR / MANAGER (see DashboardController#getDashboardStats);
     * the EmployeeDashboard intentionally does not call this — employee-self
     * dashboards source their own scoped data.
     */
    stats: async (): Promise<BackendDashboardStatsResponse> =>
      unwrapResponse<BackendDashboardStatsResponse>(await http.get('/dashboard/stats')),
  },
};
