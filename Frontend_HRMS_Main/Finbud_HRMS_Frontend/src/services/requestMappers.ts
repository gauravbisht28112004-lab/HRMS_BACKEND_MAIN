import type { Employee, EmployeeStatusLabel, EmploymentTypeLabel } from '@/types';
import type { BackendEmployeeDetailResponse } from '@/services/api';

// ---------------------------------------------------------------------------
// Option sets — keep these in sync with backend enums (see
// com.financebuddha.finbud.hrms.enums.*).
// ---------------------------------------------------------------------------

export const GENDER_OPTIONS = [
  { label: 'Select gender', value: '' },
  { label: 'Male', value: 'MALE' },
  { label: 'Female', value: 'FEMALE' },
  { label: 'Other', value: 'OTHER' },
  { label: 'Prefer not to say', value: 'PREFER_NOT_TO_SAY' },
];

export const MARITAL_STATUS_OPTIONS = [
  { label: 'Select marital status', value: '' },
  { label: 'Single', value: 'SINGLE' },
  { label: 'Married', value: 'MARRIED' },
  { label: 'Divorced', value: 'DIVORCED' },
  { label: 'Widowed', value: 'WIDOWED' },
  { label: 'Separated', value: 'SEPARATED' },
];

export const BLOOD_GROUP_OPTIONS = [
  { label: 'Select blood group', value: '' },
  { label: 'A+', value: 'A_POSITIVE' },
  { label: 'A-', value: 'A_NEGATIVE' },
  { label: 'B+', value: 'B_POSITIVE' },
  { label: 'B-', value: 'B_NEGATIVE' },
  { label: 'AB+', value: 'AB_POSITIVE' },
  { label: 'AB-', value: 'AB_NEGATIVE' },
  { label: 'O+', value: 'O_POSITIVE' },
  { label: 'O-', value: 'O_NEGATIVE' },
  { label: 'Unknown', value: 'UNKNOWN' },
];

export const EMPLOYEE_CATEGORY_OPTIONS = [
  { label: 'Select category', value: '' },
  { label: 'Permanent', value: 'PERMANENT' },
  { label: 'Contract Employee', value: 'CONTRACT_EMPLOYEE' },
  { label: 'Intern', value: 'INTERN' },
  { label: 'Consultant', value: 'CONSULTANT' },
];

export const PRODUCER_TYPE_OPTIONS = [
  { label: 'Select producer type', value: '' },
  { label: 'Producer', value: 'PRODUCER' },
  { label: 'Non-Producer', value: 'NON_PRODUCER' },
];

export const BACKGROUND_CHECK_OPTIONS = [
  { label: 'Select status', value: '' },
  { label: 'Not Initiated', value: 'NOT_INITIATED' },
  { label: 'In Progress', value: 'IN_PROGRESS' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Not Applicable', value: 'NOT_APPLICABLE' },
];

export const EMPLOYMENT_TYPE_OPTIONS: { label: string; value: EmploymentTypeLabel }[] = [
  { label: 'Full Time', value: 'Full Time' },
  { label: 'Part Time', value: 'Part Time' },
  { label: 'Contract', value: 'Contract' },
  { label: 'Intern', value: 'Intern' },
  { label: 'Probation', value: 'Probation' },
];

export const EMPLOYEE_STATUS_OPTIONS: { label: string; value: EmployeeStatusLabel }[] = [
  { label: 'Active', value: 'Active' },
  { label: 'On Leave (On Notice)', value: 'On Leave' },
  { label: 'Inactive / Terminated', value: 'Inactive' },
];

// ---------------------------------------------------------------------------
// Form shape — one flat object across every tab on the form, with
// string-typed controls so React can own the inputs without re-wiring
// per field. Numbers/booleans/enums get normalised in the mapper.
// ---------------------------------------------------------------------------

export interface EmployeeFormValues {
  // Identity
  firstName: string;
  middleName: string;
  lastName: string;
  nickName: string;
  fatherName: string;
  spouseName: string;
  dateOfBirth: string;
  gender: string;
  maritalStatus: string;
  marriageDate: string;
  bloodGroup: string;

  // Contact
  email: string;
  personalEmail: string;
  officialEmail: string;
  phone: string;
  mobileNumber: string;
  extensionNumber: string;

  // Address
  address: string;
  city: string;
  state: string;
  pincode: string;
  countryOfOrigin: string;
  location: string;
  isPhysicalChallenged: boolean;
  isInternationalEmployee: boolean;

  // Employment
  dateOfJoining: string;
  confirmDate: string;
  dateOfResignation: string;
  lastWorkingDate: string;
  departmentId: string;
  designation: string;
  managerId: string;
  employmentType: EmploymentTypeLabel;
  employeeCategory: string;
  employeeSeries: string;
  producerType: string;
  employeeReferenceNumber: string;
  costCenter: string;
  division: string;
  grade: string;
  teamName: string;
  managerNameText: string;
  branchHead: string;
  unitHead: string;
  probationPeriodDays: string;
  noticePeriodDays: string;
  shiftTypeId: string;
  status: EmployeeStatusLabel;

  // Device / login
  empCodeOnDevice: string;
  loginUsername: string;

  // Emergency contact
  emergencyContactName: string;
  emergencyContactPhone: string;
  emergencyContactRelationship: string;

  // Background verification
  backgroundCheckStatus: string;
  backgroundVerificationDate: string;
  backgroundAgencyName: string;
  backgroundCheckRemarks: string;

  // Banking
  bankAccountNumber: string;
  bankIfscCode: string;
  bankName: string;
  bankAccountType: string;
  bankBranch: string;
  salaryPaymentMode: string;
  ddPayableAt: string;
  nameAsPerBank: string;
  iban: string;

  // Statutory
  panNumber: string;
  aadhaarNumber: string;
  aadhaarEnrolmentNo: string;
  aadhaarName: string;
  uanNumber: string;
  pfEligible: boolean;
  pfNumber: string;
  pfScheme: string;
  pfJoiningDate: string;
  excessEpfEligible: boolean;
  excessEpsEligible: boolean;
  existingPfMember: boolean;
  esiEligible: boolean;
  esiNumber: string;
  lwfEligible: boolean;

  // Misc operational
  targetInfo: string;
  employeeRemarks: string;
  offerLetterIssued: string;
  idCardStatus: string;
  punchingStatus: string;
  profilePictureUrl: string;
}

export interface DepartmentFormValues {
  name: string;
  code: string;
  managerId: string;
}

export interface ShiftFormValues {
  name: string;
  code: string;
  startTime: string;
  endTime: string;
  breakDurationMinutes: string;
  gracePeriodMinutes: string;
  weeklyOffDays: string;
  overtimeThresholdHours: string;
}

export interface LeaveFormValues {
  leaveType: string;
  from: string;
  to: string;
  reason: string;
  contactDuringLeave: string;
}

// ---------------------------------------------------------------------------
// Enum mapping helpers (UI label <-> backend enum name)
// ---------------------------------------------------------------------------

const employmentTypeMap: Record<EmploymentTypeLabel, string> = {
  'Full Time': 'FULL_TIME',
  'Part Time': 'PART_TIME',
  Contract: 'CONTRACT',
  Intern: 'INTERN',
  Probation: 'PROBATION',
};

const employmentTypeFromBackend = (value: string | null | undefined): EmploymentTypeLabel => {
  switch (value) {
    case 'PART_TIME':
      return 'Part Time';
    case 'CONTRACT':
      return 'Contract';
    case 'INTERN':
      return 'Intern';
    case 'PROBATION':
      return 'Probation';
    default:
      return 'Full Time';
  }
};

const employeeStatusMap: Record<EmployeeStatusLabel, string> = {
  Active: 'ACTIVE',
  'On Leave': 'ON_NOTICE',
  Inactive: 'INACTIVE',
};

const employeeStatusFromBackend = (value: string | null | undefined): EmployeeStatusLabel => {
  switch (value) {
    case 'ON_NOTICE':
      return 'On Leave';
    case 'INACTIVE':
    case 'TERMINATED':
    case 'SUSPENDED':
      return 'Inactive';
    default:
      return 'Active';
  }
};

// ---------------------------------------------------------------------------
// Normalisers — empty strings turn into null on the wire, numerics get
// parsed or stripped, URLs are validated.
// ---------------------------------------------------------------------------

const toOptionalTrimmedValue = (value: string | null | undefined): string | null => {
  if (value === null || value === undefined) return null;
  const trimmed = String(value).trim();
  return trimmed ? trimmed : null;
};

const toOptionalEnum = (value: string | null | undefined): string | null => {
  const trimmed = toOptionalTrimmedValue(value);
  return trimmed ? trimmed : null;
};

const toOptionalInteger = (value: string | null | undefined): number | null => {
  const trimmed = toOptionalTrimmedValue(value);
  if (trimmed === null) return null;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? Math.trunc(parsed) : null;
};

const toOptionalId = (value: string | null | undefined): number | null => toOptionalInteger(value);

const toProfilePictureUrl = (value: string): string | null => {
  const trimmed = value.trim();
  if (!trimmed || trimmed.length > 500) return null;
  return /^https?:\/\//i.test(trimmed) ? trimmed : null;
};

const stringOrEmpty = (value: string | number | null | undefined): string => {
  if (value === null || value === undefined) return '';
  return String(value);
};

// ---------------------------------------------------------------------------
// Form construction — empty for "add", prefilled for "edit".
// ---------------------------------------------------------------------------

export const createEmptyEmployeeFormValues = (): EmployeeFormValues => ({
  // Identity
  firstName: '',
  middleName: '',
  lastName: '',
  nickName: '',
  fatherName: '',
  spouseName: '',
  dateOfBirth: '',
  gender: '',
  maritalStatus: '',
  marriageDate: '',
  bloodGroup: '',

  // Contact
  email: '',
  personalEmail: '',
  officialEmail: '',
  phone: '',
  mobileNumber: '',
  extensionNumber: '',

  // Address
  address: '',
  city: '',
  state: '',
  pincode: '',
  countryOfOrigin: '',
  location: '',
  isPhysicalChallenged: false,
  isInternationalEmployee: false,

  // Employment
  dateOfJoining: '',
  confirmDate: '',
  dateOfResignation: '',
  lastWorkingDate: '',
  departmentId: '',
  designation: '',
  managerId: '',
  employmentType: 'Full Time',
  employeeCategory: '',
  employeeSeries: '',
  producerType: '',
  employeeReferenceNumber: '',
  costCenter: '',
  division: '',
  grade: '',
  teamName: '',
  managerNameText: '',
  branchHead: '',
  unitHead: '',
  probationPeriodDays: '',
  noticePeriodDays: '',
  shiftTypeId: '',
  status: 'Active',

  // Device / login
  empCodeOnDevice: '',
  loginUsername: '',

  // Emergency contact
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelationship: '',

  // Background verification
  backgroundCheckStatus: '',
  backgroundVerificationDate: '',
  backgroundAgencyName: '',
  backgroundCheckRemarks: '',

  // Banking
  bankAccountNumber: '',
  bankIfscCode: '',
  bankName: '',
  bankAccountType: '',
  bankBranch: '',
  salaryPaymentMode: '',
  ddPayableAt: '',
  nameAsPerBank: '',
  iban: '',

  // Statutory
  panNumber: '',
  aadhaarNumber: '',
  aadhaarEnrolmentNo: '',
  aadhaarName: '',
  uanNumber: '',
  pfEligible: false,
  pfNumber: '',
  pfScheme: '',
  pfJoiningDate: '',
  excessEpfEligible: false,
  excessEpsEligible: false,
  existingPfMember: false,
  esiEligible: false,
  esiNumber: '',
  lwfEligible: false,

  // Misc operational
  targetInfo: '',
  employeeRemarks: '',
  offerLetterIssued: '',
  idCardStatus: '',
  punchingStatus: '',
  profilePictureUrl: '',
});

export const createEmployeeFormValuesFromEmployee = (
  employee: Employee,
  detail?: BackendEmployeeDetailResponse,
): EmployeeFormValues => {
  const raw = employee.raw;
  const bank = detail?.bankInfo;
  const identity = detail?.identityInfo;

  // Prefer raw backend fields when we have them; fall back to the
  // legacy flattened projection for mock data or half-populated rows.
  const baseFromRaw = raw
    ? {
        firstName: raw.firstName,
        middleName: stringOrEmpty(raw.middleName),
        lastName: raw.lastName,
        nickName: stringOrEmpty(raw.nickName),
        fatherName: stringOrEmpty(raw.fatherName),
        spouseName: stringOrEmpty(raw.spouseName),
        dateOfBirth: stringOrEmpty(raw.dateOfBirth),
        gender: stringOrEmpty(raw.gender),
        maritalStatus: stringOrEmpty(raw.maritalStatus),
        marriageDate: stringOrEmpty(raw.marriageDate),
        bloodGroup: stringOrEmpty(raw.bloodGroup),

        email: stringOrEmpty(raw.email),
        personalEmail: stringOrEmpty(raw.personalEmail),
        officialEmail: stringOrEmpty(raw.officialEmail),
        phone: stringOrEmpty(raw.phone),
        mobileNumber: stringOrEmpty(raw.mobileNumber),
        extensionNumber: stringOrEmpty(raw.extensionNumber),

        address: stringOrEmpty(raw.address),
        city: stringOrEmpty(raw.city),
        state: stringOrEmpty(raw.state),
        pincode: stringOrEmpty(raw.pincode),
        countryOfOrigin: stringOrEmpty(raw.countryOfOrigin),
        location: stringOrEmpty(raw.location),
        isPhysicalChallenged: Boolean(raw.isPhysicalChallenged),
        isInternationalEmployee: Boolean(raw.isInternationalEmployee),

        dateOfJoining: stringOrEmpty(raw.dateOfJoining),
        confirmDate: stringOrEmpty(raw.confirmDate),
        dateOfResignation: stringOrEmpty(raw.dateOfResignation),
        lastWorkingDate: stringOrEmpty(raw.lastWorkingDate),
        departmentId: raw.departmentId ? String(raw.departmentId) : '',
        designation: stringOrEmpty(raw.designation),
        managerId: raw.managerId ? String(raw.managerId) : '',
        employmentType: employmentTypeFromBackend(raw.employmentType),
        employeeCategory: stringOrEmpty(raw.employeeCategory),
        employeeSeries: stringOrEmpty(raw.employeeSeries),
        producerType: stringOrEmpty(raw.producerType),
        employeeReferenceNumber: stringOrEmpty(raw.employeeReferenceNumber),
        costCenter: stringOrEmpty(raw.costCenter),
        division: stringOrEmpty(raw.division),
        grade: stringOrEmpty(raw.grade),
        teamName: stringOrEmpty(raw.teamName),
        managerNameText: stringOrEmpty(raw.managerNameText),
        branchHead: stringOrEmpty(raw.branchHead),
        unitHead: stringOrEmpty(raw.unitHead),
        probationPeriodDays: raw.probationPeriodDays !== null ? String(raw.probationPeriodDays) : '',
        noticePeriodDays: raw.noticePeriodDays !== null ? String(raw.noticePeriodDays) : '',
        shiftTypeId: raw.shiftTypeId ? String(raw.shiftTypeId) : '',
        status: employeeStatusFromBackend(raw.status),

        empCodeOnDevice: raw.empCodeOnDevice !== null ? String(raw.empCodeOnDevice) : '',
        loginUsername: stringOrEmpty(raw.loginUsername),

        emergencyContactName: stringOrEmpty(raw.emergencyContactName),
        emergencyContactPhone: stringOrEmpty(raw.emergencyContactPhone),
        emergencyContactRelationship: stringOrEmpty(raw.emergencyContactRelationship),

        backgroundCheckStatus: stringOrEmpty(raw.backgroundCheckStatus),
        backgroundVerificationDate: stringOrEmpty(raw.backgroundVerificationDate),
        backgroundAgencyName: stringOrEmpty(raw.backgroundAgencyName),
        backgroundCheckRemarks: stringOrEmpty(raw.backgroundCheckRemarks),

        targetInfo: stringOrEmpty(raw.targetInfo),
        employeeRemarks: stringOrEmpty(raw.employeeRemarks),
        offerLetterIssued: stringOrEmpty(raw.offerLetterIssued),
        idCardStatus: stringOrEmpty(raw.idCardStatus),
        punchingStatus: stringOrEmpty(raw.punchingStatus),
        profilePictureUrl: stringOrEmpty(raw.profilePictureUrl),
      }
    : {
        firstName: employee.firstName,
        middleName: '',
        lastName: employee.lastName,
        nickName: '',
        fatherName: '',
        spouseName: '',
        dateOfBirth: '',
        gender: '',
        maritalStatus: '',
        marriageDate: '',
        bloodGroup: '',

        email: employee.email,
        personalEmail: '',
        officialEmail: '',
        phone: employee.phone,
        mobileNumber: '',
        extensionNumber: '',

        address: employee.address,
        city: '',
        state: '',
        pincode: '',
        countryOfOrigin: '',
        location: '',
        isPhysicalChallenged: false,
        isInternationalEmployee: false,

        dateOfJoining: employee.dateOfJoining,
        confirmDate: '',
        dateOfResignation: '',
        lastWorkingDate: '',
        departmentId: employee.departmentId ? String(employee.departmentId) : '',
        designation: employee.designation === 'Not Assigned' ? '' : employee.designation,
        managerId: employee.managerId ? String(employee.managerId) : '',
        employmentType: employee.employmentType,
        employeeCategory: '',
        employeeSeries: '',
        producerType: '',
        employeeReferenceNumber: '',
        costCenter: '',
        division: '',
        grade: '',
        teamName: '',
        managerNameText: '',
        branchHead: '',
        unitHead: '',
        probationPeriodDays: '',
        noticePeriodDays: '',
        shiftTypeId: employee.shiftTypeId ? String(employee.shiftTypeId) : '',
        status: employee.status,

        empCodeOnDevice: '',
        loginUsername: '',

        emergencyContactName: employee.emergencyContact.split(' - ')[0] ?? '',
        emergencyContactPhone: employee.emergencyContact.split(' - ')[1] ?? '',
        emergencyContactRelationship: '',

        backgroundCheckStatus: '',
        backgroundVerificationDate: '',
        backgroundAgencyName: '',
        backgroundCheckRemarks: '',

        targetInfo: '',
        employeeRemarks: '',
        offerLetterIssued: '',
        idCardStatus: '',
        punchingStatus: '',
        profilePictureUrl: employee.profilePicture ?? '',
      };

  return {
    ...baseFromRaw,

    // Banking — detail response wins, fallback to raw employee fields
    // so a backend that returns bank info directly on Employee still works.
    bankAccountNumber: bank?.accountNumber ?? '',
    bankIfscCode: bank?.ifscCode ?? '',
    bankName: bank?.bankName ?? '',
    bankAccountType: bank?.accountType ?? '',
    bankBranch: bank?.branch ?? '',
    salaryPaymentMode: bank?.salaryPaymentMode ?? '',
    ddPayableAt: bank?.ddPayableAt ?? '',
    nameAsPerBank: bank?.nameAsPerBank ?? '',
    iban: bank?.iban ?? '',

    // Statutory
    panNumber: identity?.panNumber ?? '',
    aadhaarNumber: identity?.aadhaarNumber ?? '',
    aadhaarEnrolmentNo: identity?.aadhaarEnrolmentNo ?? '',
    aadhaarName: identity?.aadhaarName ?? '',
    uanNumber: identity?.uanNumber ?? '',
    pfEligible: Boolean(identity?.pfEligible ?? raw?.pfEligible),
    pfNumber: identity?.pfNumber ?? '',
    pfScheme: identity?.pfScheme ?? '',
    pfJoiningDate: identity?.pfJoiningDate ?? '',
    excessEpfEligible: Boolean(identity?.excessEpfEligible),
    excessEpsEligible: Boolean(identity?.excessEpsEligible),
    existingPfMember: Boolean(identity?.existingPfMember),
    esiEligible: Boolean(identity?.esiEligible ?? raw?.esiEligible),
    esiNumber: identity?.esiNumber ?? '',
    lwfEligible: Boolean(identity?.lwfEligible ?? raw?.lwfEligible),
  };
};

// ---------------------------------------------------------------------------
// Form -> backend payload. Mirrors EmployeeRequest.java field-for-field.
// ---------------------------------------------------------------------------

export const mapEmployeeFormToRequest = (form: EmployeeFormValues): Record<string, unknown> => ({
  // Identity
  firstName: form.firstName.trim(),
  middleName: toOptionalTrimmedValue(form.middleName),
  lastName: form.lastName.trim(),
  nickName: toOptionalTrimmedValue(form.nickName),
  fatherName: toOptionalTrimmedValue(form.fatherName),
  spouseName: toOptionalTrimmedValue(form.spouseName),
  dateOfBirth: toOptionalTrimmedValue(form.dateOfBirth),
  gender: toOptionalEnum(form.gender),
  maritalStatus: toOptionalEnum(form.maritalStatus),
  marriageDate: toOptionalTrimmedValue(form.marriageDate),
  bloodGroup: toOptionalEnum(form.bloodGroup),

  // Contact
  email: toOptionalTrimmedValue(form.email),
  personalEmail: toOptionalTrimmedValue(form.personalEmail),
  officialEmail: toOptionalTrimmedValue(form.officialEmail),
  phone: toOptionalTrimmedValue(form.phone),
  mobileNumber: toOptionalTrimmedValue(form.mobileNumber),
  extensionNumber: toOptionalTrimmedValue(form.extensionNumber),

  // Address
  address: toOptionalTrimmedValue(form.address),
  city: toOptionalTrimmedValue(form.city),
  state: toOptionalTrimmedValue(form.state),
  pincode: toOptionalTrimmedValue(form.pincode),
  countryOfOrigin: toOptionalTrimmedValue(form.countryOfOrigin),
  location: toOptionalTrimmedValue(form.location),
  isPhysicalChallenged: Boolean(form.isPhysicalChallenged),
  isInternationalEmployee: Boolean(form.isInternationalEmployee),

  // Employment
  dateOfJoining: form.dateOfJoining,
  confirmDate: toOptionalTrimmedValue(form.confirmDate),
  dateOfResignation: toOptionalTrimmedValue(form.dateOfResignation),
  lastWorkingDate: toOptionalTrimmedValue(form.lastWorkingDate),
  departmentId: toOptionalId(form.departmentId),
  designation: toOptionalTrimmedValue(form.designation),
  managerId: toOptionalId(form.managerId),
  employmentType: employmentTypeMap[form.employmentType],
  employeeCategory: toOptionalEnum(form.employeeCategory),
  employeeSeries: toOptionalTrimmedValue(form.employeeSeries),
  producerType: toOptionalEnum(form.producerType),
  employeeReferenceNumber: toOptionalTrimmedValue(form.employeeReferenceNumber),
  costCenter: toOptionalTrimmedValue(form.costCenter),
  division: toOptionalTrimmedValue(form.division),
  grade: toOptionalTrimmedValue(form.grade),
  teamName: toOptionalTrimmedValue(form.teamName),
  managerNameText: toOptionalTrimmedValue(form.managerNameText),
  branchHead: toOptionalTrimmedValue(form.branchHead),
  unitHead: toOptionalTrimmedValue(form.unitHead),
  probationPeriodDays: toOptionalInteger(form.probationPeriodDays),
  noticePeriodDays: toOptionalInteger(form.noticePeriodDays),
  shiftTypeId: toOptionalId(form.shiftTypeId),
  status: employeeStatusMap[form.status],

  // Device / login
  empCodeOnDevice: toOptionalInteger(form.empCodeOnDevice),
  loginUsername: toOptionalTrimmedValue(form.loginUsername),

  // Emergency contact
  emergencyContactName: toOptionalTrimmedValue(form.emergencyContactName),
  emergencyContactPhone: toOptionalTrimmedValue(form.emergencyContactPhone),
  emergencyContactRelationship: toOptionalTrimmedValue(form.emergencyContactRelationship),

  // Background verification
  backgroundCheckStatus: toOptionalEnum(form.backgroundCheckStatus),
  backgroundVerificationDate: toOptionalTrimmedValue(form.backgroundVerificationDate),
  backgroundAgencyName: toOptionalTrimmedValue(form.backgroundAgencyName),
  backgroundCheckRemarks: toOptionalTrimmedValue(form.backgroundCheckRemarks),

  // Banking
  bankAccountNumber: toOptionalTrimmedValue(form.bankAccountNumber),
  bankIfscCode: toOptionalTrimmedValue(form.bankIfscCode),
  bankName: toOptionalTrimmedValue(form.bankName),
  bankAccountType: toOptionalTrimmedValue(form.bankAccountType),
  bankBranch: toOptionalTrimmedValue(form.bankBranch),
  salaryPaymentMode: toOptionalTrimmedValue(form.salaryPaymentMode),
  ddPayableAt: toOptionalTrimmedValue(form.ddPayableAt),
  nameAsPerBank: toOptionalTrimmedValue(form.nameAsPerBank),
  iban: toOptionalTrimmedValue(form.iban),

  // Statutory
  panNumber: toOptionalTrimmedValue(form.panNumber),
  aadhaarNumber: toOptionalTrimmedValue(form.aadhaarNumber),
  aadhaarEnrolmentNo: toOptionalTrimmedValue(form.aadhaarEnrolmentNo),
  aadhaarName: toOptionalTrimmedValue(form.aadhaarName),
  uanNumber: toOptionalTrimmedValue(form.uanNumber),
  pfEligible: Boolean(form.pfEligible),
  pfNumber: toOptionalTrimmedValue(form.pfNumber),
  pfScheme: toOptionalTrimmedValue(form.pfScheme),
  pfJoiningDate: toOptionalTrimmedValue(form.pfJoiningDate),
  excessEpfEligible: Boolean(form.excessEpfEligible),
  excessEpsEligible: Boolean(form.excessEpsEligible),
  existingPfMember: Boolean(form.existingPfMember),
  esiEligible: Boolean(form.esiEligible),
  esiNumber: toOptionalTrimmedValue(form.esiNumber),
  lwfEligible: Boolean(form.lwfEligible),

  // Misc operational
  targetInfo: toOptionalTrimmedValue(form.targetInfo),
  employeeRemarks: toOptionalTrimmedValue(form.employeeRemarks),
  offerLetterIssued: toOptionalTrimmedValue(form.offerLetterIssued),
  idCardStatus: toOptionalTrimmedValue(form.idCardStatus),
  punchingStatus: toOptionalTrimmedValue(form.punchingStatus),
  profilePictureUrl: toProfilePictureUrl(form.profilePictureUrl),
});

export const mapDepartmentFormToRequest = (form: DepartmentFormValues) => ({
  name: form.name.trim(),
  code: form.code.trim().toUpperCase(),
  description: '',
  managerId: form.managerId ? Number(form.managerId) : null,
});

export const mapShiftFormToRequest = (form: ShiftFormValues) => ({
  name: form.name.trim(),
  code: form.code.trim().toUpperCase(),
  startTime: form.startTime,
  endTime: form.endTime,
  breakDurationMinutes: Number(form.breakDurationMinutes || '0'),
  gracePeriodMinutes: Number(form.gracePeriodMinutes || '0'),
  weeklyOffDays: form.weeklyOffDays
    .split(',')
    .map((day) => day.trim())
    .filter(Boolean)
    .map((day) => Number(day)),
  isNightShift: false,
  overtimeThresholdHours: Number(form.overtimeThresholdHours || '0'),
});

export const mapLeaveFormToRequest = (form: LeaveFormValues) => ({
  leaveType: form.leaveType.trim().toUpperCase(),
  startDate: form.from,
  endDate: form.to,
  reason: form.reason.trim(),
  contactDuringLeave: form.contactDuringLeave.trim(),
  isHalfDay: false,
});
