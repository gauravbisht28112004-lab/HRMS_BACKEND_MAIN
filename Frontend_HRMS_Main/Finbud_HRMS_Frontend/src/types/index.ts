export type UserRole = 'Admin' | 'HR' | 'Team Leader' | 'Employee';

export interface NavItem {
  label: string;
  path: string;
  roles: UserRole[];
  icon: string;
}

export type EmploymentTypeLabel = 'Full Time' | 'Part Time' | 'Contract' | 'Intern' | 'Probation';
export type EmployeeStatusLabel = 'Active' | 'On Leave' | 'Inactive';

export interface Employee {
  id: string;
  backendId?: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;
  dateOfJoining: string;
  department: string;
  departmentId?: number;
  designation: string;
  teamLeader: string;
  managerId?: number;
  salaryStructure: string;
  employmentType: EmploymentTypeLabel;
  shiftAssignment: string;
  shiftTypeId?: number;
  status: EmployeeStatusLabel;
  emergencyContact: string;
  bankDetails: string;
  panAadhaar: string;
  performanceScore: number;
  profilePicture?: string;
  /**
   * Untouched backend payload. Screens that need Finbud-specific fields
   * (edit form, detailed profile, reports) read from `raw` rather than
   * forcing every new column into the legacy flat projection above.
   * Optional only for backwards compatibility with mock-data fixtures.
   */
  raw?: import('@/services/backendAdapters').BackendEmployeeResponse;
}

/**
 * Raw {@code AttendanceStatus} from the backend — mirrors the
 * {@code com.financebuddha.finbud.hrms.enums.AttendanceStatus} enum. Keep
 * this aligned with the server enum so the UI can render every possible
 * state (and so TypeScript complains if one is dropped).
 */
export type AttendanceBackendStatus =
  | 'PRESENT'
  | 'ABSENT'
  | 'HALF_DAY'
  | 'ON_LEAVE'
  | 'HOLIDAY'
  | 'WEEKLY_OFF'
  | 'PENDING'
  | 'AUTO_ABSENT'
  | 'MISSING_PUNCH';

export type AttendanceApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AttendanceRecord {
  id: string;
  backendId?: number;
  employeeId?: number;
  employeeCode?: string;
  employeeName: string;
  department: string;
  designation?: string;
  date: string;
  /**
   * Formatted HH:mm (or '--' when absent). For raw ISO strings that the
   * punch cards / calendars need, read {@link punchInAt} / {@link punchOutAt}.
   */
  punchIn: string;
  punchOut: string;
  /** Raw ISO-8601 instants as returned by the backend — nullable. */
  punchInAt?: string | null;
  punchOutAt?: string | null;
  workingHours: string;
  workingHoursValue?: number;
  late: boolean;
  lateMinutes?: number;
  earlyExit: boolean;
  earlyLeaveMinutes?: number;
  halfDay?: boolean;
  overtime?: boolean;
  overtimeHours: number;
  missingPunch: boolean;
  /** UI rollup used by older screens: Present / Absent / Remote. */
  status: 'Present' | 'Absent' | 'Remote';
  /** Full backend status — preferred for new surfaces that need ON_LEAVE/HOLIDAY/etc. */
  backendStatus?: AttendanceBackendStatus;
  /** Portal-punch approval state. Undefined for pre-portal rows. */
  approvalStatus?: AttendanceApprovalStatus;
  approvedById?: number;
  approvedByName?: string;
  approvedAt?: string | null;
  rejectionReason?: string;
  isAutoAbsent?: boolean;
  isMissingPunchFlag?: boolean;
  manuallyEditedById?: number;
  manuallyEditedByName?: string;
  manuallyEditedAt?: string | null;
  notes?: string;
  deviceId?: string;
  /** Geo capture for the punch (portal). */
  punchInLatitude?: number | null;
  punchInLongitude?: number | null;
  punchInAccuracyMeters?: number | null;
  punchOutLatitude?: number | null;
  punchOutLongitude?: number | null;
  punchOutAccuracyMeters?: number | null;
}

export interface OfficeLocation {
  id: number;
  name: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  geofenceRadiusMeters: number;
  enforceGeofence: boolean;
  isActive: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface PublicHoliday {
  id: number;
  holidayDate: string;
  name: string;
  description: string | null;
  isOptional: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export type RegularizationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Regularization {
  id: number;
  employeeId: number;
  employeeCode: string | null;
  employeeName: string;
  attendanceId: number | null;
  attendanceDate: string;
  requestedPunchIn: string | null;
  requestedPunchOut: string | null;
  reason: string;
  status: RegularizationStatus;
  reviewedById: number | null;
  reviewedByName: string | null;
  reviewedAt: string | null;
  reviewNotes: string | null;
  createdAt: string | null;
}

export interface Shift {
  id: string;
  backendId?: number;
  name: string;
  code?: string;
  startTime: string;
  endTime: string;
  breakTime: string;
  gracePeriod: string;
  weeklyOff: string;
  overtimeRule: string;
  assignedEmployees: string[];
  breakDurationMinutes?: number;
  gracePeriodMinutes?: number;
  weeklyOffDays?: number[];
  overtimeThresholdHours?: number;
}

export interface ShiftAssignment {
  id: number;
  employeeId: number;
  employeeCode: string | null;
  employeeName: string | null;
  shiftTypeId: number;
  shiftTypeCode: string | null;
  shiftTypeName: string | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  current: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface LeaveRequest {
  id: string;
  backendId?: number;
  employeeId?: number;
  employeeName: string;
  leaveType: string;
  from: string;
  to: string;
  days: number;
  reason: string;
  status: 'Pending' | 'Approved' | 'Rejected';
}

export type PayrollBackendStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'PAID' | 'REJECTED';

export interface PayrollEntry {
  id: string;
  backendId?: number;
  employeeId?: number;
  employeeName: string;
  month: string;
  monthNumber?: number;
  year?: number;
  department: string;
  salaryStructure: string;
  earnings: number;
  deductions: number;
  netSalary: number;
  /**
   * UI-facing rollup. "Processed" covers APPROVED + PAID, "Pending" covers
   * DRAFT + PENDING. Kept for compatibility with older screens.
   */
  status: 'Processed' | 'Pending';
  /**
   * Raw backend status. Use this when deciding what actions are legal
   * next (PENDING → Approve, APPROVED → Mark Paid, PAID → terminal).
   */
  backendStatus?: PayrollBackendStatus;
}

export interface Department {
  id: string;
  backendId?: number;
  name: string;
  code?: string;
  teamLeader: string;
  managerId?: number;
  totalEmployees: number;
}

export interface AuditLog {
  id: string;
  user: string;
  action: string;
  entity: string;
  timestamp: string;
  details: string;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  content: string;
  createdAt: string;
}

/**
 * Status of a daily-commitment row in its approval workflow. Mirrors
 * {@code com.financebuddha.finbud.hrms.enums.CommitmentStatus}. Q1 Phase A.
 */
export type CommitmentStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';

/** One commitment row per employee per workday. */
export interface DailyCommitment {
  id: number;
  employeeId: number;
  employeeCode?: string;
  employeeName?: string;
  workDate: string; // ISO yyyy-mm-dd

  targetCalls: number;
  targetOtps: number;
  targetInterestedCustomers: number;
  targetDisbursalAmount: number;

  actualCalls: number;
  actualOtps: number;
  actualInterestedCustomers: number;
  actualDisbursalAmount: number;

  status: CommitmentStatus;
  submittedAt?: string | null;
  approvedById?: number | null;
  approvedByName?: string | null;
  approvedAt?: string | null;
  rejectionReason?: string | null;
  notes?: string | null;

  createdAt?: string;
  updatedAt?: string;
}

/** One hourly activity log row. Granular self-tracking, no approval. */
export interface HourlyUpdate {
  id: number;
  employeeId: number;
  employeeCode?: string;
  employeeName?: string;
  workDate: string;
  hourSlot: string;
  callsDone: number;
  otpsAchieved: number;
  interestedCustomers: number;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Per-employee per-year leave balance after the V12 backend refactor:
 * casual + sick share a single pool, paid/earned has its own pool, LOP
 * is a tracker only.
 */
export interface LeaveBalance {
  id: number;
  employeeId: number;
  employeeName: string;
  year: number;
  casualSickAllocated: number;
  casualSickUsed: number;
  casualSickBalance: number;
  casualSickCarriedForward: number;
  paidLeaveAllocated: number;
  paidLeaveUsed: number;
  paidLeaveBalance: number;
  paidLeaveCarriedForward: number;
  lopDays: number;
}

/** Per-employee per-month sales target with the achieved overlay. */
export interface MonthlyTarget {
  id?: number; // undefined when no target has been set yet (zero-target placeholder)
  employeeId: number;
  employeeCode?: string;
  employeeName?: string;
  year: number;
  month: number;
  targetDisbursalAmount: number;
  targetLogins: number;
  achievedDisbursalAmount: number;
  achievedPercent: number;
  setById?: number | null;
  setByName?: string | null;
  notes?: string | null;
}

/** One row of the monthly disbursal leaderboard (Q3). */
export interface LeaderboardEntry {
  rank: number;
  employeeId: number;
  employeeCode?: string;
  employeeName?: string;
  department?: string | null;
  totalDisbursalAmount: number;
}

export interface Announcement {
  id: string;
  title: string;
  message: string;
  createdBy: string;
  createdAt: string;
  priority: 'High' | 'Medium' | 'Low';
  audience: UserRole[];
  imageUrl?: string;
}

export interface PerformanceMetric {
  employeeId: string;
  employeeName: string;
  teamLeader: string;
  productivityScore: number;
  qualityScore: number;
  attendanceScore: number;
  disbursalAmount: number;
  weekCompletion: number;
  monthCompletion: number;
  dailyReportingStatus: 'Submitted' | 'Pending';
}

export interface HourlyTaskLog {
  id: string;
  employeeId: string;
  date: string;
  hourSlot: string;
  task: string;
  output: string;
  status: 'Submitted' | 'Missed';
}

export interface GoalProgress {
  employeeId: string;
  dailyCommitment: string;
  weeklyTarget: string;
  monthlyTarget: string;
  dailyCompleted: number;
  weeklyCompleted: number;
  monthlyCompleted: number;
  dailyTotal: number;
  weeklyTotal: number;
  monthlyTotal: number;
  lastUpdated: string;
}
