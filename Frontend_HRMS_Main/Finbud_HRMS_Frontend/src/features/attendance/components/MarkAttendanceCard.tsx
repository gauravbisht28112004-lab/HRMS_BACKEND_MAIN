import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { StatusBadge } from '@/components/common/StatusBadge';
import { api, type AttendancePunchPayload } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import type { AttendanceRecord, OfficeLocation } from '@/types';

interface GeoReading {
  latitude: number;
  longitude: number;
  accuracy: number;
}

interface GeoResult {
  reading?: GeoReading;
  error?: string;
}

const captureLocation = async (): Promise<GeoResult> => {
  if (typeof navigator === 'undefined' || !navigator.geolocation) {
    return { error: 'Your browser does not support location access. Ask HR to mark you in manually.' };
  }

  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude, accuracy } = position.coords;
        resolve({ reading: { latitude, longitude, accuracy } });
      },
      (err) => {
        // A clear message per error code so employees can self-diagnose.
        let msg = 'Could not read your location.';
        if (err.code === err.PERMISSION_DENIED) {
          msg = 'Location access was denied. Please enable location permissions for this site and try again.';
        } else if (err.code === err.POSITION_UNAVAILABLE) {
          msg = 'Your location is temporarily unavailable. Step outside or move near a window and try again.';
        } else if (err.code === err.TIMEOUT) {
          msg = 'Location request timed out. Please try again.';
        }
        resolve({ error: msg });
      },
      { enableHighAccuracy: true, timeout: 15_000, maximumAge: 30_000 },
    );
  });
};

const approvalTone = (status: AttendanceRecord['approvalStatus']) => {
  switch (status) {
    case 'APPROVED':
      return 'success' as const;
    case 'REJECTED':
      return 'danger' as const;
    case 'PENDING':
      return 'warning' as const;
    default:
      return 'neutral' as const;
  }
};

const formatCoord = (value: number | null | undefined) =>
  value === null || value === undefined ? '—' : value.toFixed(5);

/**
 * Self-service punch card. Every authenticated employee sees this on the
 * Attendance page so they can mark themselves in/out directly from the
 * portal. The component:
 *
 *  1. Shows today's row (if any) with its current approval status so the
 *     employee knows whether their punch is still waiting for their TL.
 *  2. Captures browser geolocation before calling the backend — the
 *     backend decides whether to accept or reject based on the active
 *     office's geofence settings. We never try to short-circuit the check
 *     client-side; the server is the single source of truth.
 *  3. Surfaces backend validation errors verbatim so an out-of-geofence
 *     employee sees exactly why their punch was rejected.
 */
export const MarkAttendanceCard = () => {
  const user = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<{ kind: 'idle' | 'info' | 'error' | 'success'; message?: string }>({
    kind: 'idle',
  });
  const [lastReading, setLastReading] = useState<GeoReading | null>(null);

  const employeeDbId = user?.employeeDbId;
  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);

  const { data: todayRecord } = useQuery<AttendanceRecord | null>({
    queryKey: ['attendance-today', employeeDbId, today],
    queryFn: () => (employeeDbId ? api.attendance.getByEmployeeAndDate(employeeDbId, today) : Promise.resolve(null)),
    enabled: Boolean(employeeDbId),
    staleTime: 30_000,
  });

  const { data: offices = [] } = useQuery<OfficeLocation[]>({
    queryKey: ['office-locations-active'],
    queryFn: () => api.officeLocations.listActive(),
    staleTime: 5 * 60_000,
  });

  const punchIn = useMutation({
    mutationFn: (payload: AttendancePunchPayload) => api.attendance.punchIn(payload),
    onSuccess: async (record) => {
      // Admin / HR / TL punches auto-approve on the server, so reflect the
      // actual approvalStatus instead of always saying "awaiting approval".
      const approved = record?.approvalStatus === 'APPROVED';
      setStatus({
        kind: 'success',
        message: approved
          ? 'Punch in recorded.'
          : 'Punch in recorded. Awaiting approval.',
      });
      await queryClient.invalidateQueries({ queryKey: ['attendance-today'] });
      await queryClient.invalidateQueries({ queryKey: ['attendance'] });
    },
    onError: (err: unknown) => {
      setStatus({ kind: 'error', message: extractApiError(err, 'Could not record your punch in.') });
    },
  });

  const punchOut = useMutation({
    mutationFn: (payload: AttendancePunchPayload) => api.attendance.punchOut(payload),
    onSuccess: async () => {
      setStatus({ kind: 'success', message: 'Punch out recorded. Thanks for your work today.' });
      await queryClient.invalidateQueries({ queryKey: ['attendance-today'] });
      await queryClient.invalidateQueries({ queryKey: ['attendance'] });
    },
    onError: (err: unknown) => {
      setStatus({ kind: 'error', message: extractApiError(err, 'Could not record your punch out.') });
    },
  });

  const handlePunch = async (direction: 'in' | 'out') => {
    setStatus({ kind: 'info', message: 'Checking your location…' });
    const geo = await captureLocation();
    if (geo.error || !geo.reading) {
      setStatus({ kind: 'error', message: geo.error ?? 'Could not read your location.' });
      return;
    }
    setLastReading(geo.reading);

    const payload: AttendancePunchPayload = {
      latitude: geo.reading.latitude,
      longitude: geo.reading.longitude,
      accuracyMeters: geo.reading.accuracy,
    };

    if (direction === 'in') {
      punchIn.mutate(payload);
    } else {
      punchOut.mutate(payload);
    }
  };

  const hasPunchedIn = Boolean(todayRecord?.punchInAt);
  const hasPunchedOut = Boolean(todayRecord?.punchOutAt);
  const busy = punchIn.isPending || punchOut.isPending;

  if (!employeeDbId) {
    return (
      <Card className="border border-amber-200 bg-amber-50">
        <p className="text-sm font-semibold text-amber-800">Employee profile not linked</p>
        <p className="mt-1 text-sm text-amber-700">
          Your login isn&apos;t linked to an employee record yet. Ask HR to provision your profile before marking attendance.
        </p>
      </Card>
    );
  }

  return (
    <Card className="border border-slate-200 bg-white">
      <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-wide text-brand-700">Mark Attendance</p>
          <h2 className="text-xl font-semibold text-slate-900">Hi {user?.name ?? 'there'}, ready to start your day?</h2>
          <p className="text-sm text-slate-500">
            Every punch is reviewed by your team leader or HR. We capture your location to confirm you are on site.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <Button
            disabled={busy || hasPunchedIn}
            onClick={() => handlePunch('in')}
          >
            {hasPunchedIn ? 'Punched In' : busy && punchIn.isPending ? 'Marking In…' : 'Mark In'}
          </Button>
          <Button
            variant="secondary"
            disabled={busy || !hasPunchedIn || hasPunchedOut}
            onClick={() => handlePunch('out')}
          >
            {hasPunchedOut ? 'Punched Out' : busy && punchOut.isPending ? 'Marking Out…' : 'Mark Out'}
          </Button>
        </div>
      </div>

      <div className="mt-6 grid gap-4 md:grid-cols-4">
        <SummaryTile label="Punch In" value={todayRecord?.punchIn && todayRecord.punchIn !== '--' ? todayRecord.punchIn : 'Not yet'} />
        <SummaryTile label="Punch Out" value={todayRecord?.punchOut && todayRecord.punchOut !== '--' ? todayRecord.punchOut : 'Not yet'} />
        <SummaryTile label="Working Hours" value={todayRecord?.workingHours ?? '—'} />
        <div className="rounded-2xl bg-slate-50 p-4">
          <p className="text-sm text-slate-500">Approval</p>
          <div className="mt-2">
            {todayRecord?.approvalStatus ? (
              <StatusBadge label={todayRecord.approvalStatus} tone={approvalTone(todayRecord.approvalStatus)} />
            ) : (
              <StatusBadge label="No punch yet" tone="neutral" />
            )}
          </div>
          {todayRecord?.approvalStatus === 'REJECTED' && todayRecord.rejectionReason ? (
            <p className="mt-2 text-xs text-rose-600">{todayRecord.rejectionReason}</p>
          ) : null}
        </div>
      </div>

      {offices.length > 0 ? (
        <div className="mt-4 rounded-2xl border border-slate-100 bg-slate-50/70 p-4 text-sm text-slate-600">
          <p className="font-semibold text-slate-700">Active office locations</p>
          <ul className="mt-1 list-disc pl-5">
            {offices.map((office) => (
              <li key={office.id}>
                {office.name}
                {office.enforceGeofence
                  ? ` — geofence enforced (${office.geofenceRadiusMeters}m)`
                  : ' — geofence not enforced'}
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {status.kind !== 'idle' && status.message ? (
        <div
          className={`mt-4 rounded-2xl border px-4 py-3 text-sm ${
            status.kind === 'success'
              ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
              : status.kind === 'error'
              ? 'border-rose-200 bg-rose-50 text-rose-800'
              : 'border-sky-200 bg-sky-50 text-sky-800'
          }`}
        >
          {status.message}
        </div>
      ) : null}

      {lastReading ? (
        <p className="mt-3 text-xs text-slate-400">
          Last location: {formatCoord(lastReading.latitude)}, {formatCoord(lastReading.longitude)} · accuracy{' '}
          {Math.round(lastReading.accuracy)}m
        </p>
      ) : null}
    </Card>
  );
};

interface SummaryTileProps {
  label: string;
  value: string;
}

const SummaryTile = ({ label, value }: SummaryTileProps) => (
  <div className="rounded-2xl bg-slate-50 p-4">
    <p className="text-sm text-slate-500">{label}</p>
    <p className="mt-2 text-xl font-semibold text-slate-900">{value}</p>
  </div>
);

/**
 * Pull the most useful message out of an axios error. Backend
 * ApiResponse errors come back as `{ success: false, message: "...",
 * errors: {...} }` in the response body; we prefer the top-level
 * `message` when present so the employee sees a human-readable reason
 * (e.g. "You are 230m outside the office geofence").
 */
const extractApiError = (err: unknown, fallback: string): string => {
  const payload = (err as { response?: { data?: { message?: string; errors?: Record<string, string> } } })?.response
    ?.data;
  if (payload?.message) return payload.message;
  const firstFieldError = payload?.errors ? Object.values(payload.errors)[0] : undefined;
  if (firstFieldError) return firstFieldError;
  if (err instanceof Error) return err.message || fallback;
  return fallback;
};
