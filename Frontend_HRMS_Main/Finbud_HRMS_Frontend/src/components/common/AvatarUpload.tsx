import { useRef, useState } from 'react';
import { Camera, Trash2, UploadCloud } from 'lucide-react';
import clsx from 'clsx';
import { api } from '@/services/api';
import { Avatar } from '@/components/common/Avatar';
import { Button } from '@/components/ui/Button';

interface AvatarUploadProps {
  /** External employee code, e.g. "ND33004". Used as the path param. */
  employeeCode: string;
  /** Display name for the initials fallback. */
  name: string;
  /** Current avatar URL (presigned or legacy pasted) — undefined = none. */
  currentUrl?: string;
  /**
   * Called once the backend has persisted the change, with the new URL
   * (or undefined if the user removed the picture). The parent should use
   * this to refresh any local cache (React Query, Zustand auth store,
   * etc.) so every `<Avatar>` in the tree picks up the change.
   */
  onChange?: (newUrl: string | undefined) => void;
  /** If false, the remove/upload buttons are hidden (read-only view). */
  editable?: boolean;
}

const MAX_BYTES = 25 * 1024 * 1024; // keep in sync with finbud.storage.s3.max-upload-bytes
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

/**
 * Upload widget used on:
 *  - the employee's own profile page ("My profile")
 *  - the admin / HR employee edit modal (EmployeeForm → Profile Picture tab)
 *
 * Client-side validation mirrors the backend's allow-list so we fail fast
 * without a round-trip. Actual upload state (uploading/error) is tracked
 * locally; we don't bring React Query into this component so it works
 * both inside a query-scoped page and in plain dialogs.
 */
export const AvatarUpload = ({
  employeeCode,
  name,
  currentUrl,
  onChange,
  editable = true,
}: AvatarUploadProps) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const [displayUrl, setDisplayUrl] = useState<string | undefined>(currentUrl);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);

  const handlePick = () => inputRef.current?.click();

  const validate = (file: File): string | null => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return 'Only JPG, PNG, or WebP images are allowed.';
    }
    if (file.size > MAX_BYTES) {
      return `File is too large. Maximum size is ${MAX_BYTES / (1024 * 1024)} MB.`;
    }
    return null;
  };

  const upload = async (file: File) => {
    setError(null);
    const validation = validate(file);
    if (validation) {
      setError(validation);
      return;
    }
    try {
      setUploading(true);
      const updated = await api.employees.uploadAvatar(employeeCode, file);
      const newUrl = updated.profilePicture || undefined;
      setDisplayUrl(newUrl);
      onChange?.(newUrl);
    } catch (e) {
      const message =
        (e as { response?: { data?: { message?: string } }; message?: string })?.response?.data
          ?.message ??
        (e as Error)?.message ??
        'Upload failed. Please try again.';
      setError(message);
    } finally {
      setUploading(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) void upload(file);
    // Reset so the same file can be picked again after an error.
    e.target.value = '';
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) void upload(file);
  };

  const handleRemove = async () => {
    setError(null);
    try {
      setUploading(true);
      await api.employees.removeAvatar(employeeCode);
      setDisplayUrl(undefined);
      onChange?.(undefined);
    } catch (e) {
      const message =
        (e as { response?: { data?: { message?: string } }; message?: string })?.response?.data
          ?.message ??
        (e as Error)?.message ??
        'Failed to remove picture.';
      setError(message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-4">
        <div className="relative">
          <Avatar name={name} src={displayUrl} size="xl" className="ring-2 ring-slate-200" />
          {editable && (
            <button
              type="button"
              onClick={handlePick}
              disabled={uploading}
              className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-600 shadow-sm transition hover:text-brand-700 disabled:opacity-50"
              aria-label="Change profile picture"
            >
              <Camera size={14} />
            </button>
          )}
        </div>

        {editable && (
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setDragOver(true);
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            onClick={handlePick}
            role="button"
            tabIndex={0}
            className={clsx(
              'flex flex-1 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-4 py-6 text-center transition',
              dragOver
                ? 'border-brand-500 bg-brand-50/60 text-brand-700'
                : 'border-slate-200 bg-slate-50 text-slate-600 hover:border-brand-300 hover:bg-brand-50/40',
            )}
          >
            <UploadCloud size={20} className="mb-1" />
            <p className="text-sm font-medium">
              {uploading ? 'Uploading…' : 'Click or drag a photo here'}
            </p>
            <p className="mt-0.5 text-xs text-slate-500">JPG, PNG, or WebP · up to 25 MB</p>
          </div>
        )}
      </div>

      {editable && (
        <div className="flex flex-wrap items-center gap-2">
          <input
            ref={inputRef}
            type="file"
            accept={ALLOWED_TYPES.join(',')}
            className="hidden"
            onChange={handleFileChange}
          />
          <Button variant="secondary" onClick={handlePick} disabled={uploading}>
            {displayUrl ? 'Change photo' : 'Upload photo'}
          </Button>
          {displayUrl && (
            <Button variant="ghost" onClick={handleRemove} disabled={uploading} className="gap-2 text-red-600">
              <Trash2 size={14} />
              Remove
            </Button>
          )}
        </div>
      )}

      {error && (
        <p className="text-sm text-red-600" role="alert">
          {error}
        </p>
      )}
    </div>
  );
};
