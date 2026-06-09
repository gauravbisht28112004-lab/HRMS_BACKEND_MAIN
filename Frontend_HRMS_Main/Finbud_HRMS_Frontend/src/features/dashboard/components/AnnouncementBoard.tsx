import { useState } from 'react';
import { Megaphone, Trash2 } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Announcement, UserRole } from '@/types';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { StatusBadge } from '@/components/common/StatusBadge';
import { api } from '@/services/api';

interface AnnouncementBoardProps {
  /**
   * Role context — kept for API compatibility with the existing call sites
   * (dashboards pass this), but the backend doesn't filter by audience.
   * Every authenticated user sees every active announcement (Q2 design,
   * locked 2026-04-25).
   */
  role: UserRole;
  /** When true, render the publish-new-announcement form. Admin/HR pass this. */
  canCreate?: boolean;
}

type DraftPriority = 'HIGH' | 'MEDIUM' | 'LOW';

const FORM_INITIAL = {
  title: '',
  message: '',
  priority: 'MEDIUM' as DraftPriority,
};

/**
 * Real-data announcement board. Fetches active announcements from
 * `/api/announcements` and lets Admin/HR publish or archive notices via
 * the same endpoint family. Replaces the prior in-memory mock list.
 */
export const AnnouncementBoard = ({ canCreate = false }: AnnouncementBoardProps) => {
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState(FORM_INITIAL);
  const [error, setError] = useState<string | null>(null);

  const { data: items = [], isLoading } = useQuery<Announcement[]>({
    queryKey: ['announcements-active'],
    queryFn: () => api.announcements.listActive(),
    staleTime: 30_000,
  });

  const createMutation = useMutation({
    mutationFn: () =>
      api.announcements.create({
        title: draft.title.trim(),
        message: draft.message.trim(),
        priority: draft.priority,
      }),
    onSuccess: () => {
      setDraft(FORM_INITIAL);
      setError(null);
      queryClient.invalidateQueries({ queryKey: ['announcements-active'] });
    },
    onError: (err: unknown) => {
      const apiMessage = (err as { response?: { data?: { message?: string } } }).response?.data?.message;
      setError(apiMessage ?? (err instanceof Error ? err.message : 'Could not publish announcement.'));
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (id: number) => api.announcements.deactivate(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['announcements-active'] }),
  });

  const submit = () => {
    if (!draft.title.trim() || !draft.message.trim()) {
      setError('Title and message are required.');
      return;
    }
    createMutation.mutate();
  };

  return (
    <div className="space-y-6">
      {canCreate ? (
        <Card>
          <div className="flex items-center gap-3">
            <div className="rounded-2xl bg-brand-50 p-3 text-brand-700">
              <Megaphone size={20} />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-slate-900">Announcement Center</h3>
              <p className="text-sm text-slate-500">Published announcements appear on every dashboard.</p>
            </div>
          </div>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            <Input
              label="Title"
              value={draft.title}
              onChange={(event) => setDraft({ ...draft, title: event.target.value })}
              placeholder="Office closed on Friday"
            />
            <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
              <span>Priority</span>
              <select
                className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
                value={draft.priority}
                onChange={(event) => setDraft({ ...draft, priority: event.target.value as DraftPriority })}
              >
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </label>
            <div className="md:col-span-2">
              <label className="flex flex-col gap-2 text-sm font-medium text-slate-700">
                <span>Message</span>
                <textarea
                  rows={4}
                  value={draft.message}
                  onChange={(event) => setDraft({ ...draft, message: event.target.value })}
                  placeholder="Detail about the announcement…"
                  className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
                />
              </label>
            </div>
            {error ? (
              <div className="md:col-span-2 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
                {error}
              </div>
            ) : null}
            <div className="md:col-span-2 flex justify-end">
              <Button onClick={submit} disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Publishing…' : 'Publish Announcement'}
              </Button>
            </div>
          </div>
        </Card>
      ) : null}

      <Card>
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-slate-900">Announcements</h3>
            <p className="text-sm text-slate-500">Published by Admin / HR.</p>
          </div>
          <StatusBadge label={`${items.length} active`} tone="info" />
        </div>
        <div className="mt-5 space-y-4">
          {isLoading ? (
            <p className="text-sm text-slate-500">Loading announcements…</p>
          ) : items.length === 0 ? (
            <p className="text-sm text-slate-500">No announcements yet.</p>
          ) : (
            items.map((item) => (
              <div key={item.id} className="rounded-2xl border border-slate-100 bg-slate-50 p-4">
                <div className="flex items-center justify-between gap-3">
                  <h4 className="font-semibold text-slate-900">{item.title}</h4>
                  <div className="flex items-center gap-2">
                    <StatusBadge
                      label={item.priority}
                      tone={item.priority === 'High' ? 'danger' : item.priority === 'Medium' ? 'warning' : 'info'}
                    />
                    {canCreate ? (
                      <button
                        onClick={() => archiveMutation.mutate(Number(item.id))}
                        disabled={archiveMutation.isPending}
                        title="Archive announcement"
                        className="rounded-lg border border-slate-200 bg-white p-1.5 text-slate-500 hover:bg-rose-50 hover:text-rose-600 disabled:opacity-50"
                      >
                        <Trash2 size={14} />
                      </button>
                    ) : null}
                  </div>
                </div>
                <p className="mt-2 whitespace-pre-line text-sm text-slate-600">{item.message}</p>
                <p className="mt-3 text-xs text-slate-500">
                  {item.createdBy} • {item.createdAt}
                </p>
              </div>
            ))
          )}
        </div>
      </Card>
    </div>
  );
};
