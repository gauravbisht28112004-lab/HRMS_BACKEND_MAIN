import { FormEvent, useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { DataTable } from '@/components/common/DataTable';
import { PageHeader } from '@/components/common/PageHeader';
import { StatusBadge } from '@/components/common/StatusBadge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import type { OfficeLocationPayload } from '@/services/api';
import { api } from '@/services/api';
import type { OfficeLocation } from '@/types';

/**
 * HR / Admin screen for managing the office locations that power the
 * MarkAttendanceCard geofence. Each row has:
 *   - name / address (label employees see on their punch card)
 *   - latitude / longitude (decimal degrees, 5+ decimals recommended)
 *   - geofenceRadiusMeters (how close an employee must be)
 *   - enforceGeofence (hard-deny out-of-range punches when true)
 *   - isActive (soft-disable without deleting — archived rows don't show up
 *     in the employee-facing punch card)
 *
 * We intentionally avoid destructive "delete" actions by default — toggling
 * `isActive = false` is the right way to retire a location without losing
 * the historical audit trail. The delete button is kept for truly
 * mis-created rows.
 */

interface FormState {
  id?: number;
  name: string;
  address: string;
  latitude: string;
  longitude: string;
  geofenceRadiusMeters: string;
  enforceGeofence: boolean;
  isActive: boolean;
}

const blankForm: FormState = {
  name: '',
  address: '',
  latitude: '',
  longitude: '',
  geofenceRadiusMeters: '200',
  enforceGeofence: false,
  isActive: true,
};

const toPayload = (form: FormState): OfficeLocationPayload => ({
  name: form.name.trim(),
  address: form.address.trim() || undefined,
  latitude: form.latitude.trim() ? Number(form.latitude) : undefined,
  longitude: form.longitude.trim() ? Number(form.longitude) : undefined,
  geofenceRadiusMeters: form.geofenceRadiusMeters.trim()
    ? Number(form.geofenceRadiusMeters)
    : undefined,
  enforceGeofence: form.enforceGeofence,
  isActive: form.isActive,
});

const fromOffice = (office: OfficeLocation): FormState => ({
  id: office.id,
  name: office.name,
  address: office.address ?? '',
  latitude: office.latitude != null ? String(office.latitude) : '',
  longitude: office.longitude != null ? String(office.longitude) : '',
  geofenceRadiusMeters: String(office.geofenceRadiusMeters ?? 200),
  enforceGeofence: office.enforceGeofence,
  isActive: office.isActive,
});

export const OfficeLocationsPage = () => {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<FormState>(blankForm);
  const [error, setError] = useState<string | null>(null);
  const [capturing, setCapturing] = useState(false);

  const { data: offices = [], isFetching } = useQuery<OfficeLocation[]>({
    queryKey: ['office-locations'],
    queryFn: () => api.officeLocations.list(),
  });

  // Clear the stale error banner any time the user tweaks the form so the
  // banner isn't "frozen" from a previous submission.
  useEffect(() => {
    if (error) setError(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.name, form.latitude, form.longitude, form.geofenceRadiusMeters]);

  const createMutation = useMutation({
    mutationFn: () => api.officeLocations.create(toPayload(form)),
    onSuccess: () => {
      setForm(blankForm);
      queryClient.invalidateQueries({ queryKey: ['office-locations'] });
      queryClient.invalidateQueries({ queryKey: ['office-locations-active'] });
    },
    onError: (err: unknown) => setError(extractError(err, 'Could not create office location.')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id }: { id: number }) => api.officeLocations.update(id, toPayload(form)),
    onSuccess: () => {
      setForm(blankForm);
      queryClient.invalidateQueries({ queryKey: ['office-locations'] });
      queryClient.invalidateQueries({ queryKey: ['office-locations-active'] });
    },
    onError: (err: unknown) => setError(extractError(err, 'Could not update office location.')),
  });

  const removeMutation = useMutation({
    mutationFn: (id: number) => api.officeLocations.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['office-locations'] });
      queryClient.invalidateQueries({ queryKey: ['office-locations-active'] });
    },
  });

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!form.name.trim()) {
      setError('Name is required.');
      return;
    }
    if (form.enforceGeofence && (!form.latitude.trim() || !form.longitude.trim())) {
      setError('Latitude and longitude are required when geofence is enforced.');
      return;
    }
    if (form.id) {
      updateMutation.mutate({ id: form.id });
    } else {
      createMutation.mutate();
    }
  };

  /**
   * One-click "use my current location" helper — HR usually opens this page
   * from the office laptop, so the browser-reported lat/long is the exact
   * right value. We keep this on the UI only (never called by the
   * punch card) so the seeded value always goes through HR explicitly.
   */
  const useMyLocation = () => {
    if (!navigator.geolocation) {
      setError('Your browser does not expose geolocation.');
      return;
    }
    setCapturing(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setForm((prev) => ({
          ...prev,
          latitude: pos.coords.latitude.toFixed(6),
          longitude: pos.coords.longitude.toFixed(6),
        }));
        setCapturing(false);
      },
      (err) => {
        setError(err.message || 'Could not read your current location.');
        setCapturing(false);
      },
      { enableHighAccuracy: true, timeout: 15_000 },
    );
  };

  const busy = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Office locations"
        description="Configure the offices employees can punch in from. Enable geofence to hard-deny punches more than N metres away."
      />

      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <Card>
          <h3 className="text-lg font-semibold text-slate-900">
            {form.id ? 'Edit office location' : 'Add office location'}
          </h3>
          <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
            <Input
              label="Name"
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              placeholder="Mumbai HQ"
              required
            />
            <Input
              label="Address"
              value={form.address}
              onChange={(event) => setForm({ ...form, address: event.target.value })}
              placeholder="Andheri West, Mumbai"
            />
            <div className="grid gap-4 md:grid-cols-2">
              <Input
                label="Latitude"
                value={form.latitude}
                onChange={(event) => setForm({ ...form, latitude: event.target.value })}
                placeholder="19.13682"
                inputMode="decimal"
              />
              <Input
                label="Longitude"
                value={form.longitude}
                onChange={(event) => setForm({ ...form, longitude: event.target.value })}
                placeholder="72.82613"
                inputMode="decimal"
              />
            </div>
            <Input
              label="Geofence radius (metres)"
              type="number"
              value={form.geofenceRadiusMeters}
              onChange={(event) => setForm({ ...form, geofenceRadiusMeters: event.target.value })}
              min={10}
            />
            <div className="flex flex-wrap gap-4">
              <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                <input
                  type="checkbox"
                  checked={form.enforceGeofence}
                  onChange={(event) => setForm({ ...form, enforceGeofence: event.target.checked })}
                />
                Enforce geofence
              </label>
              <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                <input
                  type="checkbox"
                  checked={form.isActive}
                  onChange={(event) => setForm({ ...form, isActive: event.target.checked })}
                />
                Active
              </label>
            </div>

            {error ? (
              <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
                {error}
              </div>
            ) : null}

            <div className="flex flex-wrap items-center gap-3">
              <Button type="submit" disabled={busy}>
                {busy ? 'Saving…' : form.id ? 'Update location' : 'Create location'}
              </Button>
              <Button type="button" variant="secondary" onClick={useMyLocation} disabled={capturing || busy}>
                {capturing ? 'Reading location…' : 'Use my current location'}
              </Button>
              {form.id ? (
                <Button type="button" variant="ghost" onClick={() => setForm(blankForm)}>
                  Cancel edit
                </Button>
              ) : null}
            </div>
          </form>
        </Card>

        <Card>
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-slate-900">Configured offices</h3>
              <p className="text-sm text-slate-500">Active rows surface on every employee's punch card.</p>
            </div>
            <StatusBadge
              label={`${offices.filter((o) => o.isActive).length} active`}
              tone={offices.some((o) => o.isActive) ? 'success' : 'neutral'}
            />
          </div>
          <div className="mt-4">
            <DataTable
              data={offices}
              emptyTitle={isFetching ? 'Loading…' : 'No offices yet'}
              emptyDescription={isFetching ? 'Fetching locations.' : 'Add your first office from the form.'}
              columns={[
                {
                  key: 'name',
                  header: 'Name',
                  render: (record) => (
                    <div className="space-y-0.5">
                      <p className="font-medium text-slate-900">{record.name}</p>
                      {record.address ? <p className="text-xs text-slate-500">{record.address}</p> : null}
                    </div>
                  ),
                },
                {
                  key: 'coords',
                  header: 'Coordinates',
                  render: (record) =>
                    record.latitude != null && record.longitude != null
                      ? `${record.latitude.toFixed(5)}, ${record.longitude.toFixed(5)}`
                      : '—',
                },
                {
                  key: 'geofence',
                  header: 'Geofence',
                  render: (record) =>
                    record.enforceGeofence ? (
                      <StatusBadge label={`${record.geofenceRadiusMeters}m`} tone="info" />
                    ) : (
                      <StatusBadge label="Advisory" tone="neutral" />
                    ),
                },
                {
                  key: 'status',
                  header: 'Status',
                  render: (record) => (
                    <StatusBadge label={record.isActive ? 'Active' : 'Inactive'} tone={record.isActive ? 'success' : 'neutral'} />
                  ),
                },
                {
                  key: 'actions',
                  header: '',
                  render: (record) => (
                    <div className="flex items-center gap-2">
                      <Button variant="secondary" onClick={() => setForm(fromOffice(record))}>
                        Edit
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={() => {
                          if (window.confirm(`Delete ${record.name}? Prefer marking it inactive instead.`)) {
                            removeMutation.mutate(record.id);
                          }
                        }}
                      >
                        Delete
                      </Button>
                    </div>
                  ),
                },
              ]}
            />
          </div>
        </Card>
      </div>
    </div>
  );
};

const extractError = (err: unknown, fallback: string): string => {
  const payload = (err as { response?: { data?: { message?: string } } } | undefined)?.response?.data;
  if (payload?.message) return payload.message;
  if (err instanceof Error && err.message) return err.message;
  return fallback;
};
