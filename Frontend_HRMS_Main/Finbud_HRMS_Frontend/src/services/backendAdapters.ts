import type {
  Employee,
  AttendanceRecord,
  AttendanceApprovalStatus,
  AttendanceBackendStatus,
  Shift,
  ShiftAssignment,
  Announcement,
  CommitmentStatus,
  DailyCommitment,
  HourlyUpdate,
  LeaderboardEntry,
  LeaveBalance,
  MonthlyTarget,
  LeaveRequest,
  PayrollEntry,
  Department,
  OfficeLocation,
  PublicHoliday,
  Regularization,
  RegularizationStatus,
  UserRole,
} from '@/types';
import { mapBackendRolesToUiRoles, pickPrimaryRole } from '@/services/roleMapping';

export interface BackendLoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  employeeId: string | null;
  email: string | null;
  fullName: string | null;
  roles: string[];
  /**
   * Backend-driven hint: when true the user was provisioned with the
   * default/temporary password and must rotate it before proceeding.
   * Optional on the wire — older backends that don't emit it are treated
   * as `false`.
   */
  mustChangePassword?: boolean;
}

/**
 * Mirrors com.financebuddha.finbud.hrms.dto.employee.EmployeeResponse
 * verbatim so the full Finbud profile can round-trip through the UI.
 * Every enum field is typed as `string | null` because the backend
 * serialises them as their enum NAMEs (e.g. "MALE", "A_POSITIVE",
 * "FULL_TIME", "ACTIVE"). Mapping to the UI-friendly labels happens in
 * `toEmployee` / the form mappers.
 */
export interface BackendEmployeeResponse {
  id: number;
  employeeId: string;

  // Identity
  firstName: string;
  middleName: string | null;
  lastName: string;
  fullName: string | null;
  nickName: string | null;
  fatherName: string | null;
  spouseName: string | null;

  dateOfBirth: string | null;
  gender: string | null;
  maritalStatus: string | null;
  marriageDate: string | null;
  bloodGroup: string | null;

  // Contact
  email: string | null;
  personalEmail: string | null;
  officialEmail: string | null;
  phone: string | null;
  mobileNumber: string | null;
  extensionNumber: string | null;

  address: string | null;
  city: string | null;
  state: string | null;
  pincode: string | null;
  countryOfOrigin: string | null;
  location: string | null;

  isPhysicalChallenged: boolean | null;
  isInternationalEmployee: boolean | null;

  // Employment
  dateOfJoining: string;
  confirmDate: string | null;
  dateOfResignation: string | null;
  lastWorkingDate: string | null;

  departmentId: number | null;
  departmentName: string | null;

  designation: string | null;

  managerId: number | null;
  managerName: string | null;

  employmentType: string | null;
  employeeCategory: string | null;
  employeeSeries: string | null;
  producerType: string | null;
  employeeReferenceNumber: string | null;
  costCenter: string | null;
  division: string | null;
  grade: string | null;
  teamName: string | null;
  managerNameText: string | null;
  branchHead: string | null;
  unitHead: string | null;
  probationPeriodDays: number | null;
  noticePeriodDays: number | null;

  status: string | null;

  shiftTypeId: number | null;
  shiftName: string | null;

  // Device / login
  empCodeOnDevice: number | null;
  loginUsername: string | null;

  // Emergency contact
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  emergencyContactRelationship: string | null;

  // Background verification
  backgroundCheckStatus: string | null;
  backgroundVerificationDate: string | null;
  backgroundAgencyName: string | null;
  backgroundCheckRemarks: string | null;

  // Statutory flags (full numbers live in EmployeeDetailResponse)
  pfEligible: boolean | null;
  esiEligible: boolean | null;
  lwfEligible: boolean | null;

  // Misc operational
  targetInfo: string | null;
  employeeRemarks: string | null;
  offerLetterIssued: string | null;
  idCardStatus: string | null;
  punchingStatus: string | null;
  profilePictureUrl: string | null;

  createdAt: string | null;
  updatedAt: string | null;
}

export interface BackendDepartmentResponse {
  id: number;
  name: string;
  code: string;
  description: string | null;
  managerId: number | null;
  managerName: string | null;
  employeeCount: number | null;
}

export interface BackendShiftResponse {
  id: number;
  name: string;
  code: string;
  startTime: string;
  endTime: string;
  breakDurationMinutes: number | null;
  gracePeriodMinutes: number | null;
  weeklyOffDays: number[] | null;
  overtimeThresholdHours: number | null;
}

export interface BackendShiftAssignmentResponse {
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

export interface BackendAttendanceResponse {
  id: number;
  employeeId: number;
  employeeCode: string | null;
  employeeName: string;
  department: string | null;
  designation: string | null;
  attendanceDate: string;
  punchIn: string | null;
  punchOut: string | null;
  workingHours: number | null;
  status: string;
  isLate: boolean | null;
  lateMinutes: number | null;
  isEarlyLeave: boolean | null;
  earlyLeaveMinutes: number | null;
  isHalfDay: boolean | null;
  isOvertime: boolean | null;
  overtimeHours: number | null;
  deviceId: string | null;
  notes: string | null;
  // Geo / approval surface
  punchInLatitude: number | null;
  punchInLongitude: number | null;
  punchInAccuracyMeters: number | null;
  punchOutLatitude: number | null;
  punchOutLongitude: number | null;
  punchOutAccuracyMeters: number | null;
  approvalStatus: string | null;
  approvedById: number | null;
  approvedByName: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  isAutoAbsent: boolean | null;
  isMissingPunch: boolean | null;
  manuallyEditedById: number | null;
  manuallyEditedByName: string | null;
  manuallyEditedAt: string | null;
}

export interface BackendOfficeLocationResponse {
  id: number;
  name: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  geofenceRadiusMeters: number | null;
  enforceGeofence: boolean | null;
  isActive: boolean | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface BackendPublicHolidayResponse {
  id: number;
  holidayDate: string;
  name: string;
  description: string | null;
  isOptional: boolean | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface BackendRegularizationResponse {
  id: number;
  employeeId: number;
  employeeCode: string | null;
  employeeName: string;
  attendanceId: number | null;
  attendanceDate: string;
  requestedPunchIn: string | null;
  requestedPunchOut: string | null;
  reason: string;
  status: string;
  reviewedById: number | null;
  reviewedByName: string | null;
  reviewedAt: string | null;
  reviewNotes: string | null;
  createdAt: string | null;
}

export interface BackendLeaveResponse {
  id: number;
  employeeId: number;
  employeeName: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  daysRequested: number | null;
  reason: string;
  status: string;
}

export interface BackendPayrollResponse {
  id: number;
  employeeId: number;
  employeeName: string;
  departmentName: string | null;
  month: number;
  year: number;
  monthYear: string;
  grossEarnings: number | null;
  totalDeductions: number | null;
  netPay: number | null;
  status: string;
}

export interface AuthUser {
  username: string;
  name: string;
  email: string;
  role: UserRole;
  roles: UserRole[];
  backendRoles: string[];
  employeeId?: string;
  employeeDbId?: number;
  hasUsedSelfServiceEdit: boolean;
  mustChangePassword: boolean;
  accessTokenExpiresAt?: number;
  /**
   * Presigned S3 GET URL for the user's profile picture, hydrated from
   * the employee enrichment fetch. Presigned URLs have a finite TTL
   * (finbud.storage.s3.presign-ttl-seconds on the backend); the Topbar /
   * Avatar components re-fetch on session refresh. Undefined = fall back
   * to initials.
   */
  avatarUrl?: string;
}

const titleCase = (value: string) =>
  value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');

const formatTime = (value: string | null) => {
  if (!value) return '--';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 5);
  }

  return date.toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
};

const formatWorkingHours = (value: number | null) => {
  if (value === null || Number.isNaN(value)) return '0h';

  const hours = Math.floor(value);
  const minutes = Math.round((value - hours) * 60);
  return `${hours}h ${minutes}m`;
};

const mapEmploymentType = (value: string | null): Employee['employmentType'] => {
  switch (value) {
    case 'CONTRACT':
      return 'Contract';
    case 'INTERN':
      return 'Intern';
    case 'PART_TIME':
      return 'Part Time';
    case 'PROBATION':
      return 'Probation';
    default:
      return 'Full Time';
  }
};

const mapEmployeeStatus = (value: string | null): Employee['status'] => {
  switch (value) {
    case 'INACTIVE':
    case 'TERMINATED':
    case 'SUSPENDED':
      return 'Inactive';
    case 'ON_NOTICE':
      return 'On Leave';
    default:
      return 'Active';
  }
};

const mapAttendanceStatus = (value: string): AttendanceRecord['status'] => {
  switch (value) {
    case 'ABSENT':
    case 'AUTO_ABSENT':
      return 'Absent';
    case 'PENDING':
    case 'MISSING_PUNCH':
      return 'Remote';
    default:
      return 'Present';
  }
};

const mapBackendAttendanceStatus = (value: string | null): AttendanceBackendStatus | undefined => {
  if (!value) return undefined;
  switch (value) {
    case 'PRESENT':
    case 'ABSENT':
    case 'HALF_DAY':
    case 'ON_LEAVE':
    case 'HOLIDAY':
    case 'WEEKLY_OFF':
    case 'PENDING':
    case 'AUTO_ABSENT':
    case 'MISSING_PUNCH':
      return value;
    default:
      return undefined;
  }
};

const mapApprovalStatus = (value: string | null): AttendanceApprovalStatus | undefined => {
  if (!value) return undefined;
  switch (value) {
    case 'PENDING':
    case 'APPROVED':
    case 'REJECTED':
      return value;
    default:
      return undefined;
  }
};

const mapRegularizationStatus = (value: string): RegularizationStatus => {
  switch (value) {
    case 'APPROVED':
      return 'APPROVED';
    case 'REJECTED':
      return 'REJECTED';
    default:
      return 'PENDING';
  }
};

const toNullableNumber = (value: number | null | undefined): number | null => {
  if (value === null || value === undefined || Number.isNaN(value)) return null;
  return value;
};

const mapLeaveStatus = (value: string): LeaveRequest['status'] => {
  switch (value) {
    case 'APPROVED':
      return 'Approved';
    case 'REJECTED':
    case 'CANCELLED':
    case 'WITHDRAWN':
      return 'Rejected';
    default:
      return 'Pending';
  }
};

const mapPayrollStatus = (value: string): PayrollEntry['status'] =>
  value === 'PAID' || value === 'APPROVED' ? 'Processed' : 'Pending';

const mapPayrollBackendStatus = (value: string): PayrollEntry['backendStatus'] => {
  switch (value) {
    case 'DRAFT':
    case 'PENDING':
    case 'APPROVED':
    case 'PAID':
    case 'REJECTED':
      return value;
    default:
      return 'PENDING';
  }
};

const weeklyOffLabel = (weeklyOffDays: number[] | null) => {
  if (!weeklyOffDays?.length) return 'Not Set';

  const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  return weeklyOffDays.map((day) => days[day] ?? `Day ${day}`).join(', ');
};

export const toAuthUser = (response: BackendLoginResponse): AuthUser => {
  const roles = mapBackendRolesToUiRoles(response.roles);
  const expiresInMs = Number.isFinite(response.expiresIn) ? response.expiresIn * 1000 : undefined;
  const accessTokenExpiresAt = expiresInMs ? Date.now() + expiresInMs : undefined;

  return {
    username: response.username,
    name: response.fullName ?? response.username,
    email: response.email ?? '',
    role: pickPrimaryRole(roles),
    roles,
    backendRoles: response.roles,
    employeeId: response.employeeId ?? undefined,
    hasUsedSelfServiceEdit: false,
    mustChangePassword: Boolean(response.mustChangePassword),
    accessTokenExpiresAt,
  };
};

export const toEmployee = (response: BackendEmployeeResponse): Employee => ({
  id: response.employeeId,
  backendId: response.id,
  firstName: response.firstName,
  lastName: response.lastName,
  email: response.email ?? '',
  phone: response.phone ?? '',
  address: [response.address, response.city, response.state, response.pincode].filter(Boolean).join(', '),
  dateOfJoining: response.dateOfJoining,
  department: response.departmentName ?? 'Unassigned',
  departmentId: response.departmentId ?? undefined,
  designation: response.designation ?? 'Not Assigned',
  teamLeader: response.managerName ?? 'Not Assigned',
  managerId: response.managerId ?? undefined,
  salaryStructure: 'Managed in backend',
  employmentType: mapEmploymentType(response.employmentType),
  shiftAssignment: response.shiftName ?? 'Unassigned',
  shiftTypeId: response.shiftTypeId ?? undefined,
  status: mapEmployeeStatus(response.status),
  emergencyContact: [response.emergencyContactName, response.emergencyContactPhone].filter(Boolean).join(' - '),
  bankDetails: 'Managed securely in backend',
  panAadhaar: 'Protected',
  performanceScore: 0,
  profilePicture: response.profilePictureUrl ?? '',

  // Carry the raw backend payload so screens that need the full set
  // (edit form, detail card, reports) don't have to re-fetch.
  raw: response,
});

export const toDepartment = (response: BackendDepartmentResponse): Department => ({
  id: String(response.id),
  backendId: response.id,
  name: response.name,
  code: response.code,
  teamLeader: response.managerName ?? 'Not Assigned',
  managerId: response.managerId ?? undefined,
  totalEmployees: response.employeeCount ?? 0,
});

export const toShift = (response: BackendShiftResponse): Shift => ({
  id: String(response.id),
  backendId: response.id,
  name: response.name,
  code: response.code,
  startTime: response.startTime.slice(0, 5),
  endTime: response.endTime.slice(0, 5),
  breakTime: `${response.breakDurationMinutes ?? 0} mins`,
  gracePeriod: `${response.gracePeriodMinutes ?? 0} mins`,
  weeklyOff: weeklyOffLabel(response.weeklyOffDays),
  overtimeRule: `After ${response.overtimeThresholdHours ?? 0} hours`,
  assignedEmployees: [],
  breakDurationMinutes: response.breakDurationMinutes ?? 0,
  gracePeriodMinutes: response.gracePeriodMinutes ?? 0,
  weeklyOffDays: response.weeklyOffDays ?? [],
  overtimeThresholdHours: response.overtimeThresholdHours ?? 0,
});

export const toShiftAssignment = (response: BackendShiftAssignmentResponse): ShiftAssignment => ({
  id: response.id,
  employeeId: response.employeeId,
  employeeCode: response.employeeCode,
  employeeName: response.employeeName,
  shiftTypeId: response.shiftTypeId,
  shiftTypeCode: response.shiftTypeCode,
  shiftTypeName: response.shiftTypeName,
  effectiveFrom: response.effectiveFrom,
  effectiveTo: response.effectiveTo,
  current: Boolean(response.current),
  createdAt: response.createdAt,
  updatedAt: response.updatedAt,
});

export const toAttendanceRecord = (response: BackendAttendanceResponse): AttendanceRecord => ({
  id: String(response.id),
  backendId: response.id,
  employeeId: response.employeeId,
  employeeCode: response.employeeCode ?? undefined,
  employeeName: response.employeeName,
  department: response.department ?? 'Assigned via employee profile',
  designation: response.designation ?? undefined,
  date: response.attendanceDate,
  punchIn: formatTime(response.punchIn),
  punchOut: formatTime(response.punchOut),
  punchInAt: response.punchIn,
  punchOutAt: response.punchOut,
  workingHours: formatWorkingHours(response.workingHours),
  workingHoursValue: response.workingHours ?? 0,
  late: Boolean(response.isLate),
  lateMinutes: response.lateMinutes ?? 0,
  earlyExit: Boolean(response.isEarlyLeave),
  earlyLeaveMinutes: response.earlyLeaveMinutes ?? 0,
  halfDay: Boolean(response.isHalfDay),
  overtime: Boolean(response.isOvertime),
  overtimeHours: response.overtimeHours ?? 0,
  // Prefer the server's explicit flag; fall back to "missing one side of a punch pair".
  missingPunch:
    response.isMissingPunch !== null && response.isMissingPunch !== undefined
      ? response.isMissingPunch
      : (response.status === 'MISSING_PUNCH' || (!!response.punchIn && !response.punchOut)),
  status: mapAttendanceStatus(response.status),
  backendStatus: mapBackendAttendanceStatus(response.status),
  approvalStatus: mapApprovalStatus(response.approvalStatus),
  approvedById: response.approvedById ?? undefined,
  approvedByName: response.approvedByName ?? undefined,
  approvedAt: response.approvedAt,
  rejectionReason: response.rejectionReason ?? undefined,
  isAutoAbsent: response.isAutoAbsent ?? false,
  isMissingPunchFlag: response.isMissingPunch ?? false,
  manuallyEditedById: response.manuallyEditedById ?? undefined,
  manuallyEditedByName: response.manuallyEditedByName ?? undefined,
  manuallyEditedAt: response.manuallyEditedAt,
  notes: response.notes ?? undefined,
  deviceId: response.deviceId ?? undefined,
  punchInLatitude: toNullableNumber(response.punchInLatitude),
  punchInLongitude: toNullableNumber(response.punchInLongitude),
  punchInAccuracyMeters: toNullableNumber(response.punchInAccuracyMeters),
  punchOutLatitude: toNullableNumber(response.punchOutLatitude),
  punchOutLongitude: toNullableNumber(response.punchOutLongitude),
  punchOutAccuracyMeters: toNullableNumber(response.punchOutAccuracyMeters),
});

export const toOfficeLocation = (response: BackendOfficeLocationResponse): OfficeLocation => ({
  id: response.id,
  name: response.name,
  address: response.address,
  latitude: toNullableNumber(response.latitude),
  longitude: toNullableNumber(response.longitude),
  geofenceRadiusMeters: response.geofenceRadiusMeters ?? 100,
  enforceGeofence: Boolean(response.enforceGeofence),
  isActive: response.isActive ?? true,
  createdAt: response.createdAt,
  updatedAt: response.updatedAt,
});

export const toPublicHoliday = (response: BackendPublicHolidayResponse): PublicHoliday => ({
  id: response.id,
  holidayDate: response.holidayDate,
  name: response.name,
  description: response.description,
  isOptional: Boolean(response.isOptional),
  createdAt: response.createdAt,
  updatedAt: response.updatedAt,
});

export const toRegularization = (response: BackendRegularizationResponse): Regularization => ({
  id: response.id,
  employeeId: response.employeeId,
  employeeCode: response.employeeCode,
  employeeName: response.employeeName,
  attendanceId: response.attendanceId,
  attendanceDate: response.attendanceDate,
  requestedPunchIn: response.requestedPunchIn,
  requestedPunchOut: response.requestedPunchOut,
  reason: response.reason,
  status: mapRegularizationStatus(response.status),
  reviewedById: response.reviewedById,
  reviewedByName: response.reviewedByName,
  reviewedAt: response.reviewedAt,
  reviewNotes: response.reviewNotes,
  createdAt: response.createdAt,
});

/**
 * Mirror of {@code com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentResponse}.
 * Numeric BigDecimal fields come over as plain numbers from Spring's
 * default Jackson config.
 */
export interface BackendDailyCommitmentResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  workDate: string;
  targetCalls: number;
  targetOtps: number;
  targetInterestedCustomers: number;
  targetDisbursalAmount: number;
  actualCalls: number;
  actualOtps: number;
  actualInterestedCustomers: number;
  actualDisbursalAmount: number;
  status: CommitmentStatus;
  submittedAt: string | null;
  approvedById: number | null;
  approvedByName: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export const toDailyCommitment = (response: BackendDailyCommitmentResponse): DailyCommitment => ({
  id: response.id,
  employeeId: response.employeeId,
  employeeCode: response.employeeCode,
  employeeName: response.employeeName,
  workDate: response.workDate,
  targetCalls: response.targetCalls ?? 0,
  targetOtps: response.targetOtps ?? 0,
  targetInterestedCustomers: response.targetInterestedCustomers ?? 0,
  targetDisbursalAmount: Number(response.targetDisbursalAmount ?? 0),
  actualCalls: response.actualCalls ?? 0,
  actualOtps: response.actualOtps ?? 0,
  actualInterestedCustomers: response.actualInterestedCustomers ?? 0,
  actualDisbursalAmount: Number(response.actualDisbursalAmount ?? 0),
  status: response.status,
  submittedAt: response.submittedAt,
  approvedById: response.approvedById,
  approvedByName: response.approvedByName,
  approvedAt: response.approvedAt,
  rejectionReason: response.rejectionReason,
  notes: response.notes,
  createdAt: response.createdAt,
  updatedAt: response.updatedAt,
});

/** Mirror of {@code HourlyUpdateResponse}. */
export interface BackendHourlyUpdateResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  workDate: string;
  hourSlot: string;
  callsDone: number;
  otpsAchieved: number;
  interestedCustomers: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export const toHourlyUpdate = (response: BackendHourlyUpdateResponse): HourlyUpdate => ({
  id: response.id,
  employeeId: response.employeeId,
  employeeCode: response.employeeCode,
  employeeName: response.employeeName,
  workDate: response.workDate,
  hourSlot: response.hourSlot,
  callsDone: response.callsDone ?? 0,
  otpsAchieved: response.otpsAchieved ?? 0,
  interestedCustomers: response.interestedCustomers ?? 0,
  notes: response.notes,
  createdAt: response.createdAt,
  updatedAt: response.updatedAt,
});

/** Mirror of {@code LeaveBalanceResponse} — V12 combined-pool shape. */
export interface BackendLeaveBalanceResponse {
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

export const toLeaveBalance = (response: BackendLeaveBalanceResponse): LeaveBalance => ({
  id: response.id,
  employeeId: response.employeeId,
  employeeName: response.employeeName,
  year: response.year,
  casualSickAllocated: Number(response.casualSickAllocated ?? 0),
  casualSickUsed: Number(response.casualSickUsed ?? 0),
  casualSickBalance: Number(response.casualSickBalance ?? 0),
  casualSickCarriedForward: Number(response.casualSickCarriedForward ?? 0),
  paidLeaveAllocated: Number(response.paidLeaveAllocated ?? 0),
  paidLeaveUsed: Number(response.paidLeaveUsed ?? 0),
  paidLeaveBalance: Number(response.paidLeaveBalance ?? 0),
  paidLeaveCarriedForward: Number(response.paidLeaveCarriedForward ?? 0),
  lopDays: Number(response.lopDays ?? 0),
});

/** Mirror of {@code MonthlyTargetResponse}. */
export interface BackendMonthlyTargetResponse {
  id: number | null;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  year: number;
  month: number;
  targetDisbursalAmount: number;
  targetLogins: number;
  achievedDisbursalAmount: number;
  achievedPercent: number;
  setById: number | null;
  setByName: string | null;
  notes: string | null;
}

export const toMonthlyTarget = (response: BackendMonthlyTargetResponse): MonthlyTarget => ({
  id: response.id ?? undefined,
  employeeId: response.employeeId,
  employeeCode: response.employeeCode,
  employeeName: response.employeeName,
  year: response.year,
  month: response.month,
  targetDisbursalAmount: Number(response.targetDisbursalAmount ?? 0),
  targetLogins: response.targetLogins ?? 0,
  achievedDisbursalAmount: Number(response.achievedDisbursalAmount ?? 0),
  achievedPercent: response.achievedPercent ?? 0,
  setById: response.setById,
  setByName: response.setByName,
  notes: response.notes,
});

/** Mirror of {@code LeaderboardEntryResponse}. */
export interface BackendLeaderboardEntryResponse {
  rank: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  department: string | null;
  totalDisbursalAmount: number;
}

export const toLeaderboardEntry = (response: BackendLeaderboardEntryResponse): LeaderboardEntry => ({
  rank: response.rank,
  employeeId: response.employeeId,
  employeeCode: response.employeeCode,
  employeeName: response.employeeName,
  department: response.department,
  totalDisbursalAmount: Number(response.totalDisbursalAmount ?? 0),
});

/** Mirror of {@code com.financebuddha.finbud.hrms.dto.announcement.AnnouncementResponse}. */
export interface BackendAnnouncementResponse {
  id: number;
  title: string;
  message: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  isActive: boolean;
  createdById: number;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Map a backend announcement to the existing {@link Announcement} type.
 * The frontend type predates the backend by months — it uses title-case
 * priority ("High"/"Medium"/"Low") and an audience array. We coerce
 * priority and supply a default "all roles" audience because the backend
 * doesn't (yet) support per-role targeting; everyone with a login sees
 * every active announcement.
 */
export const toAnnouncement = (response: BackendAnnouncementResponse): Announcement => ({
  id: String(response.id),
  title: response.title,
  message: response.message,
  createdBy: response.createdByName,
  // Backend uses ISO-8601; format for the UI's "name • when" line.
  createdAt: response.createdAt
    ? new Date(response.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
    : '',
  priority: titleCase(response.priority) as Announcement['priority'],
  audience: ['Admin', 'HR', 'Team Leader', 'Employee'],
});

export const toLeaveRequest = (response: BackendLeaveResponse): LeaveRequest => ({
  id: String(response.id),
  backendId: response.id,
  employeeId: response.employeeId,
  employeeName: response.employeeName,
  leaveType: titleCase(response.leaveType),
  from: response.startDate,
  to: response.endDate,
  days: response.daysRequested ?? 0,
  reason: response.reason,
  status: mapLeaveStatus(response.status),
});


export const toPayrollEntry = (response: BackendPayrollResponse): PayrollEntry => ({
  id: String(response.id),
  backendId: response.id,
  employeeId: response.employeeId,
  employeeName: response.employeeName,
  month: response.monthYear,
  monthNumber: response.month,
  year: response.year,
  department: response.departmentName ?? 'Unassigned',
  salaryStructure: 'Backend managed',
  earnings: response.grossEarnings ?? 0,
  deductions: response.totalDeductions ?? 0,
  netSalary: response.netPay ?? 0,
  status: mapPayrollStatus(response.status),
  backendStatus: mapPayrollBackendStatus(response.status),
});
