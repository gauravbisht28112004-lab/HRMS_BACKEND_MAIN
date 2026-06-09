import { ChangeEvent, FormEvent, ReactNode, useEffect, useMemo, useState } from 'react';
import { AvatarUpload } from '@/components/common/AvatarUpload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import {
  BACKGROUND_CHECK_OPTIONS,
  BLOOD_GROUP_OPTIONS,
  EMPLOYEE_CATEGORY_OPTIONS,
  EMPLOYEE_STATUS_OPTIONS,
  EMPLOYMENT_TYPE_OPTIONS,
  GENDER_OPTIONS,
  MARITAL_STATUS_OPTIONS,
  PRODUCER_TYPE_OPTIONS,
  createEmptyEmployeeFormValues,
  type EmployeeFormValues,
} from '@/services/requestMappers';
import { SalaryTab } from './SalaryTab';

interface EmployeeFormProps {
  initialValues?: EmployeeFormValues;
  onSubmit: (values: EmployeeFormValues) => Promise<unknown> | void;
  onCancel?: () => void;
  isSubmitting?: boolean;
  isEditMode?: boolean;
  editingLabel?: string;
  /**
   * External employee code (e.g. "ND33004") of the employee being edited.
   * Only needed in edit mode so the AvatarUpload widget can post to
   * `/api/employees/{code}/avatar`. When creating a new employee we
   * don't have a code yet, so avatar upload is hidden for creates.
   */
  employeeCode?: string;
  /**
   * Called by AvatarUpload after a successful upload/remove. The parent
   * should refresh its employee list cache so the new avatar shows up
   * in table rows / profile screens.
   */
  onAvatarChange?: (newUrl: string | undefined) => void;
  departmentOptions: { label: string; value: string }[];
  managerOptions: { label: string; value: string }[];
  shiftOptions: { label: string; value: string }[];
  /**
   * Backend primary key of the employee being edited. Required to surface
   * the Salary tab because the salary API is keyed by the numeric employee
   * id, not the external employee code. `null`/`undefined` on creates.
   */
  backendEmployeeId?: number | null;
  /**
   * Whether the current user can view & edit the salary structure.
   * Only Admin and HR should see the Salary tab at all.
   */
  canEditSalary?: boolean;
}

type TabKey =
  | 'identity'
  | 'contact'
  | 'employment'
  | 'login'
  | 'emergency'
  | 'background'
  | 'banking'
  | 'statutory'
  | 'salary'
  | 'misc';

interface TabDefinition {
  key: TabKey;
  label: string;
  summary: string;
}

const BASE_TABS: TabDefinition[] = [
  { key: 'identity', label: 'Identity', summary: 'Name, DOB, gender, family' },
  { key: 'contact', label: 'Contact & Address', summary: 'Emails, phones, location' },
  { key: 'employment', label: 'Employment', summary: 'Role, department, dates' },
  { key: 'login', label: 'Device & Login', summary: 'Punch device, portal user' },
  { key: 'emergency', label: 'Emergency', summary: 'Next-of-kin contact' },
  { key: 'background', label: 'Background', summary: 'Verification status' },
  { key: 'banking', label: 'Banking', summary: 'Salary account details' },
  { key: 'statutory', label: 'Statutory', summary: 'PAN, Aadhaar, PF, ESI, LWF' },
  { key: 'misc', label: 'Miscellaneous', summary: 'Target, remarks, documents' },
];

const SALARY_TAB: TabDefinition = {
  key: 'salary',
  label: 'Salary & CTC',
  summary: 'Monthly CTC, NTH, statutory deductions, incentives',
};

const CheckboxRow = ({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}) => (
  <label className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-700">
    <input
      type="checkbox"
      checked={checked}
      onChange={(event) => onChange(event.target.checked)}
      className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-300"
    />
    <span>{label}</span>
  </label>
);

const TextArea = ({
  label,
  value,
  onChange,
  rows = 3,
}: {
  label: string;
  value: string;
  onChange: (event: ChangeEvent<HTMLTextAreaElement>) => void;
  rows?: number;
}) => (
  <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
    <span>{label}</span>
    <textarea
      value={value}
      onChange={onChange}
      rows={rows}
      className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
    />
  </label>
);

const Section = ({ title, children }: { title: string; children: ReactNode }) => (
  <div>
    <h4 className="text-sm font-semibold uppercase tracking-wider text-brand-700">{title}</h4>
    <div className="mt-3">{children}</div>
  </div>
);

export const EmployeeForm = ({
  initialValues,
  onSubmit,
  onCancel,
  isSubmitting = false,
  isEditMode = false,
  editingLabel,
  employeeCode,
  onAvatarChange,
  departmentOptions,
  managerOptions,
  shiftOptions,
  backendEmployeeId,
  canEditSalary = false,
}: EmployeeFormProps) => {
  const [form, setForm] = useState<EmployeeFormValues>(initialValues ?? createEmptyEmployeeFormValues());
  const [activeTab, setActiveTab] = useState<TabKey>('identity');

  // Only HR/Admin, and only in edit mode (so we have a backend id), can see salary.
  const showSalaryTab = isEditMode && canEditSalary;
  const TABS = useMemo<TabDefinition[]>(
    () => (showSalaryTab ? [...BASE_TABS, SALARY_TAB] : BASE_TABS),
    [showSalaryTab],
  );

  // If the user was on the salary tab and it gets hidden (e.g. they
  // switch to a create flow), bounce them back to identity to avoid a
  // stale activeTab that renders nothing.
  useEffect(() => {
    if (!showSalaryTab && activeTab === 'salary') {
      setActiveTab('identity');
    }
  }, [showSalaryTab, activeTab]);

  useEffect(() => {
    const next = initialValues ?? createEmptyEmployeeFormValues();
    setForm(next);
  }, [initialValues]);

  const updateField =
    <K extends keyof EmployeeFormValues>(key: K) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
      setForm((prev) => ({ ...prev, [key]: event.target.value } as EmployeeFormValues));
    };

  const setBoolField = <K extends keyof EmployeeFormValues>(key: K) => (value: boolean) => {
    setForm((prev) => ({ ...prev, [key]: value } as EmployeeFormValues));
  };

  const fullName = [form.firstName, form.middleName, form.lastName].filter(Boolean).join(' ').trim();

  const handleAvatarChange = (newUrl: string | undefined) => {
    // Keep the form's mirror of the URL in sync so downstream submits see it,
    // and let the parent refresh its list/query caches.
    setForm((prev) => ({ ...prev, profilePictureUrl: newUrl ?? '' }));
    onAvatarChange?.(newUrl);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await onSubmit(form);
  };

  const tabSummary = useMemo(
    () => TABS.find((tab) => tab.key === activeTab)?.summary ?? '',
    [TABS, activeTab],
  );

  return (
    <Card>
      <div className="flex flex-col gap-4 border-b border-slate-100 pb-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.25em] text-brand-600">
            {isEditMode ? 'Edit Employee' : 'Add Employee'}
          </p>
          <h3 className="mt-1 text-xl font-semibold text-slate-900">
            {isEditMode ? editingLabel ?? 'Update employee record' : 'Create new employee'}
          </h3>
          <p className="mt-1 text-sm text-slate-500">
            All fields map 1:1 to the backend Finbud employee master. {tabSummary}
          </p>
        </div>
        {onCancel && isEditMode ? (
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancel edit
          </Button>
        ) : null}
      </div>

      <div className="mt-5 flex flex-wrap gap-2 border-b border-slate-100 pb-4">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setActiveTab(tab.key)}
            className={`rounded-full border px-4 py-1.5 text-xs font-medium transition ${
              activeTab === tab.key
                ? 'border-brand-500 bg-brand-50 text-brand-700'
                : 'border-slate-200 bg-white text-slate-600 hover:border-brand-200 hover:text-brand-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'salary' ? (
        <div className="mt-6">
          <SalaryTab
            employeeBackendId={backendEmployeeId ?? null}
            canEdit={canEditSalary}
          />
        </div>
      ) : null}

      <form
        onSubmit={handleSubmit}
        className={`mt-6 space-y-6 ${activeTab === 'salary' ? 'hidden' : ''}`}
      >
        {activeTab === 'identity' ? (
          <Section title="Identity">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <Input label="First Name *" value={form.firstName} onChange={updateField('firstName')} required />
              <Input label="Middle Name" value={form.middleName} onChange={updateField('middleName')} />
              <Input label="Last Name *" value={form.lastName} onChange={updateField('lastName')} required />
              <Input label="Nick Name" value={form.nickName} onChange={updateField('nickName')} />
              <Input label="Father's Name" value={form.fatherName} onChange={updateField('fatherName')} />
              <Input label="Spouse Name" value={form.spouseName} onChange={updateField('spouseName')} />
              <Input label="Date of Birth" type="date" value={form.dateOfBirth} onChange={updateField('dateOfBirth')} />
              <Select label="Gender" value={form.gender} onChange={updateField('gender')} options={GENDER_OPTIONS} />
              <Select
                label="Marital Status"
                value={form.maritalStatus}
                onChange={updateField('maritalStatus')}
                options={MARITAL_STATUS_OPTIONS}
              />
              <Input
                label="Marriage Date"
                type="date"
                value={form.marriageDate}
                onChange={updateField('marriageDate')}
              />
              <Select
                label="Blood Group"
                value={form.bloodGroup}
                onChange={updateField('bloodGroup')}
                options={BLOOD_GROUP_OPTIONS}
              />
            </div>
          </Section>
        ) : null}

        {activeTab === 'contact' ? (
          <div className="space-y-6">
            <Section title="Contact">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input label="Official Email" type="email" value={form.email} onChange={updateField('email')} />
                <Input
                  label="Personal Email"
                  type="email"
                  value={form.personalEmail}
                  onChange={updateField('personalEmail')}
                />
                <Input
                  label="Alternate Official Email"
                  type="email"
                  value={form.officialEmail}
                  onChange={updateField('officialEmail')}
                />
                <Input label="Phone" value={form.phone} onChange={updateField('phone')} />
                <Input label="Mobile Number" value={form.mobileNumber} onChange={updateField('mobileNumber')} />
                <Input
                  label="Extension Number"
                  value={form.extensionNumber}
                  onChange={updateField('extensionNumber')}
                />
              </div>
            </Section>

            <Section title="Address">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input label="City" value={form.city} onChange={updateField('city')} />
                <Input label="State" value={form.state} onChange={updateField('state')} />
                <Input label="Pincode" value={form.pincode} onChange={updateField('pincode')} />
                <Input
                  label="Country of Origin"
                  value={form.countryOfOrigin}
                  onChange={updateField('countryOfOrigin')}
                />
                <Input label="Work Location" value={form.location} onChange={updateField('location')} />
              </div>
              <div className="mt-4">
                <TextArea label="Address" value={form.address} onChange={updateField('address')} rows={3} />
              </div>
              <div className="mt-4 grid gap-4 md:grid-cols-2">
                <CheckboxRow
                  label="Physically challenged"
                  checked={form.isPhysicalChallenged}
                  onChange={setBoolField('isPhysicalChallenged')}
                />
                <CheckboxRow
                  label="International employee"
                  checked={form.isInternationalEmployee}
                  onChange={setBoolField('isInternationalEmployee')}
                />
              </div>
            </Section>
          </div>
        ) : null}

        {activeTab === 'employment' ? (
          <div className="space-y-6">
            <Section title="Core employment">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input
                  label="Date of Joining *"
                  type="date"
                  value={form.dateOfJoining}
                  onChange={updateField('dateOfJoining')}
                  required
                />
                <Input
                  label="Confirmation Date"
                  type="date"
                  value={form.confirmDate}
                  onChange={updateField('confirmDate')}
                />
                <Input
                  label="Date of Resignation"
                  type="date"
                  value={form.dateOfResignation}
                  onChange={updateField('dateOfResignation')}
                />
                <Input
                  label="Last Working Date"
                  type="date"
                  value={form.lastWorkingDate}
                  onChange={updateField('lastWorkingDate')}
                />
                <Select
                  label="Department"
                  value={form.departmentId}
                  onChange={updateField('departmentId')}
                  options={departmentOptions}
                />
                <Input label="Designation" value={form.designation} onChange={updateField('designation')} />
                <Select
                  label="Reporting Manager"
                  value={form.managerId}
                  onChange={updateField('managerId')}
                  options={managerOptions}
                />
                <Select
                  label="Shift Assignment"
                  value={form.shiftTypeId}
                  onChange={updateField('shiftTypeId')}
                  options={shiftOptions}
                />
                <Select
                  label="Employment Type"
                  value={form.employmentType}
                  onChange={updateField('employmentType')}
                  options={EMPLOYMENT_TYPE_OPTIONS}
                />
                <Select
                  label="Employee Category"
                  value={form.employeeCategory}
                  onChange={updateField('employeeCategory')}
                  options={EMPLOYEE_CATEGORY_OPTIONS}
                />
                <Select
                  label="Status"
                  value={form.status}
                  onChange={updateField('status')}
                  options={EMPLOYEE_STATUS_OPTIONS}
                />
                <Select
                  label="Producer Type"
                  value={form.producerType}
                  onChange={updateField('producerType')}
                  options={PRODUCER_TYPE_OPTIONS}
                />
              </div>
            </Section>

            <Section title="Finbud-specific classification">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input
                  label="Employee Series"
                  value={form.employeeSeries}
                  onChange={updateField('employeeSeries')}
                />
                <Input
                  label="Employee Reference #"
                  value={form.employeeReferenceNumber}
                  onChange={updateField('employeeReferenceNumber')}
                />
                <Input label="Cost Center" value={form.costCenter} onChange={updateField('costCenter')} />
                <Input label="Division" value={form.division} onChange={updateField('division')} />
                <Input label="Grade" value={form.grade} onChange={updateField('grade')} />
                <Input label="Team Name" value={form.teamName} onChange={updateField('teamName')} />
                <Input
                  label="Manager Name (Text)"
                  value={form.managerNameText}
                  onChange={updateField('managerNameText')}
                />
                <Input label="Branch Head" value={form.branchHead} onChange={updateField('branchHead')} />
                <Input label="Unit Head" value={form.unitHead} onChange={updateField('unitHead')} />
                <Input
                  label="Probation Period (days)"
                  type="number"
                  min={0}
                  value={form.probationPeriodDays}
                  onChange={updateField('probationPeriodDays')}
                />
                <Input
                  label="Notice Period (days)"
                  type="number"
                  min={0}
                  value={form.noticePeriodDays}
                  onChange={updateField('noticePeriodDays')}
                />
              </div>
            </Section>
          </div>
        ) : null}

        {activeTab === 'login' ? (
          <Section title="Device & login">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <Input
                label="Employee Code on Device"
                type="number"
                value={form.empCodeOnDevice}
                onChange={updateField('empCodeOnDevice')}
              />
              <Input
                label="Portal Login Username"
                value={form.loginUsername}
                onChange={updateField('loginUsername')}
              />
            </div>
            <div className="mt-6 rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-medium text-slate-700">Profile Picture</p>
              {isEditMode && employeeCode ? (
                <>
                  <p className="mt-1 text-xs text-slate-500">
                    Upload a photo for this employee. It will be stored securely in object storage and shown across
                    the portal (topbar, profile, employee directory).
                  </p>
                  <div className="mt-3">
                    <AvatarUpload
                      employeeCode={employeeCode}
                      name={fullName || editingLabel || 'Employee'}
                      currentUrl={form.profilePictureUrl || undefined}
                      onChange={handleAvatarChange}
                    />
                  </div>
                </>
              ) : (
                <p className="mt-1 text-xs text-slate-500">
                  Save the employee first — once the record exists you can come back here to upload a profile photo.
                </p>
              )}
            </div>
          </Section>
        ) : null}

        {activeTab === 'emergency' ? (
          <Section title="Emergency contact">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <Input
                label="Emergency Contact Name"
                value={form.emergencyContactName}
                onChange={updateField('emergencyContactName')}
              />
              <Input
                label="Emergency Contact Phone"
                value={form.emergencyContactPhone}
                onChange={updateField('emergencyContactPhone')}
              />
              <Input
                label="Relationship"
                value={form.emergencyContactRelationship}
                onChange={updateField('emergencyContactRelationship')}
              />
            </div>
          </Section>
        ) : null}

        {activeTab === 'background' ? (
          <Section title="Background verification">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <Select
                label="Status"
                value={form.backgroundCheckStatus}
                onChange={updateField('backgroundCheckStatus')}
                options={BACKGROUND_CHECK_OPTIONS}
              />
              <Input
                label="Verification Date"
                type="date"
                value={form.backgroundVerificationDate}
                onChange={updateField('backgroundVerificationDate')}
              />
              <Input
                label="Agency Name"
                value={form.backgroundAgencyName}
                onChange={updateField('backgroundAgencyName')}
              />
            </div>
            <div className="mt-4">
              <TextArea
                label="Remarks"
                value={form.backgroundCheckRemarks}
                onChange={updateField('backgroundCheckRemarks')}
                rows={3}
              />
            </div>
          </Section>
        ) : null}

        {activeTab === 'banking' ? (
          <Section title="Banking">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <Input
                label="Account Number"
                value={form.bankAccountNumber}
                onChange={updateField('bankAccountNumber')}
              />
              <Input label="IFSC Code" value={form.bankIfscCode} onChange={updateField('bankIfscCode')} />
              <Input label="Bank Name" value={form.bankName} onChange={updateField('bankName')} />
              <Input
                label="Account Type"
                value={form.bankAccountType}
                onChange={updateField('bankAccountType')}
                placeholder="Savings / Current"
              />
              <Input label="Branch" value={form.bankBranch} onChange={updateField('bankBranch')} />
              <Input
                label="Salary Payment Mode"
                value={form.salaryPaymentMode}
                onChange={updateField('salaryPaymentMode')}
              />
              <Input label="DD Payable At" value={form.ddPayableAt} onChange={updateField('ddPayableAt')} />
              <Input
                label="Name as per Bank"
                value={form.nameAsPerBank}
                onChange={updateField('nameAsPerBank')}
              />
              <Input label="IBAN" value={form.iban} onChange={updateField('iban')} />
            </div>
          </Section>
        ) : null}

        {activeTab === 'statutory' ? (
          <div className="space-y-6">
            <Section title="Identity numbers">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input label="PAN Number" value={form.panNumber} onChange={updateField('panNumber')} />
                <Input label="Aadhaar Number" value={form.aadhaarNumber} onChange={updateField('aadhaarNumber')} />
                <Input
                  label="Aadhaar Enrolment #"
                  value={form.aadhaarEnrolmentNo}
                  onChange={updateField('aadhaarEnrolmentNo')}
                />
                <Input
                  label="Name on Aadhaar"
                  value={form.aadhaarName}
                  onChange={updateField('aadhaarName')}
                />
                <Input label="UAN Number" value={form.uanNumber} onChange={updateField('uanNumber')} />
              </div>
            </Section>

            <Section title="Provident Fund (PF)">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input label="PF Number" value={form.pfNumber} onChange={updateField('pfNumber')} />
                <Input label="PF Scheme" value={form.pfScheme} onChange={updateField('pfScheme')} />
                <Input
                  label="PF Joining Date"
                  type="date"
                  value={form.pfJoiningDate}
                  onChange={updateField('pfJoiningDate')}
                />
              </div>
              <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                <CheckboxRow
                  label="PF eligible"
                  checked={form.pfEligible}
                  onChange={setBoolField('pfEligible')}
                />
                <CheckboxRow
                  label="Existing PF member"
                  checked={form.existingPfMember}
                  onChange={setBoolField('existingPfMember')}
                />
                <CheckboxRow
                  label="Excess EPF eligible"
                  checked={form.excessEpfEligible}
                  onChange={setBoolField('excessEpfEligible')}
                />
                <CheckboxRow
                  label="Excess EPS eligible"
                  checked={form.excessEpsEligible}
                  onChange={setBoolField('excessEpsEligible')}
                />
              </div>
            </Section>

            <Section title="ESI & LWF">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                <Input label="ESI Number" value={form.esiNumber} onChange={updateField('esiNumber')} />
              </div>
              <div className="mt-4 grid gap-3 md:grid-cols-2">
                <CheckboxRow
                  label="ESI eligible"
                  checked={form.esiEligible}
                  onChange={setBoolField('esiEligible')}
                />
                <CheckboxRow
                  label="LWF eligible"
                  checked={form.lwfEligible}
                  onChange={setBoolField('lwfEligible')}
                />
              </div>
            </Section>
          </div>
        ) : null}

        {activeTab === 'misc' ? (
          <Section title="Miscellaneous operational">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <Input label="Target Info" value={form.targetInfo} onChange={updateField('targetInfo')} />
              <Input
                label="Offer Letter Issued"
                value={form.offerLetterIssued}
                onChange={updateField('offerLetterIssued')}
                placeholder="Yes / No / Pending"
              />
              <Input
                label="ID Card Status"
                value={form.idCardStatus}
                onChange={updateField('idCardStatus')}
                placeholder="Issued / Pending"
              />
              <Input
                label="Punching Status"
                value={form.punchingStatus}
                onChange={updateField('punchingStatus')}
                placeholder="Enabled / Disabled"
              />
            </div>
            <div className="mt-4">
              <TextArea
                label="Employee Remarks"
                value={form.employeeRemarks}
                onChange={updateField('employeeRemarks')}
                rows={4}
              />
            </div>
          </Section>
        ) : null}

        <div className="flex flex-col gap-3 border-t border-slate-100 pt-4 md:flex-row md:items-center md:justify-between">
          <p className="text-xs text-slate-500">
            Required fields: First Name, Last Name, Date of Joining. All other fields are optional and only get sent
            when you fill them in.
          </p>
          <div className="flex justify-end gap-3">
            {onCancel ? (
              <Button type="button" variant="secondary" onClick={onCancel}>
                {isEditMode ? 'Cancel' : 'Reset'}
              </Button>
            ) : (
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  const next = initialValues ?? createEmptyEmployeeFormValues();
                  setForm(next);
                }}
              >
                Reset
              </Button>
            )}
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving...' : isEditMode ? 'Save changes' : 'Create employee'}
            </Button>
          </div>
        </div>
      </form>
    </Card>
  );
};
