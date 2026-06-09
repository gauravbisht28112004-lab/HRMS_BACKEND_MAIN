import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, X } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { useAuthStore } from '@/store/authStore';

/**
 * Shows a single, dismissible warning when the signed-in user is still
 * on the default/temporary password (backend flag
 * {@code mustChangePassword=true}). The user can either rotate the
 * password right away or continue with the default — but either way the
 * modal only appears once per account on this device.
 *
 * The previous behaviour hard-gated the whole app behind a forced
 * /force-change-password redirect. That blocks admins from doing first-day
 * setup tasks (Excel import, user provisioning) just because their own
 * account hasn't been rotated yet. Softening it to a one-time nag keeps
 * the security reminder visible without blocking real work.
 */
const STORAGE_KEY_PREFIX = 'finbud.passwordWarningAcknowledged.';

const acknowledgementKey = (user: { username?: string; employeeId?: string } | null) => {
  if (!user) return null;
  // username is the stable backend-side identifier; fall back to employeeId
  // for any edge cases where it is missing.
  const id = user.username || user.employeeId;
  return id ? STORAGE_KEY_PREFIX + id : null;
};

export const PasswordChangeWarningModal = () => {
  const user = useAuthStore((state) => state.user);
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (!user?.mustChangePassword) {
      setOpen(false);
      return;
    }
    const key = acknowledgementKey(user);
    if (!key) return;
    try {
      if (window.localStorage.getItem(key) === 'true') {
        setOpen(false);
        return;
      }
    } catch {
      // localStorage can throw in private/SSR contexts — fall through and
      // show the modal rather than silently skip the warning.
    }
    setOpen(true);
  }, [user]);

  if (!open || !user?.mustChangePassword) {
    return null;
  }

  const acknowledge = () => {
    const key = acknowledgementKey(user);
    if (key) {
      try {
        window.localStorage.setItem(key, 'true');
      } catch {
        // Best-effort; if storage is unavailable the modal will show again
        // next visit, which is acceptable for a security nag.
      }
    }
    setOpen(false);
  };

  const changeNow = () => {
    acknowledge();
    navigate('/force-change-password');
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="password-warning-title"
      className="fixed inset-0 z-[80] flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm"
    >
      <div className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl">
        <div className="flex items-start gap-4">
          <div className="rounded-2xl bg-amber-50 p-3 text-amber-600">
            <AlertTriangle size={24} />
          </div>
          <div className="flex-1">
            <h2 id="password-warning-title" className="text-lg font-semibold text-slate-900">
              You are still on the default password
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              Your account is using the temporary password issued at provisioning. Anyone who knows this default can
              sign in as you. We strongly recommend rotating it now, but you can continue to the portal and change it
              later from your profile. This reminder will not appear again on this device.
            </p>
          </div>
          <button
            onClick={acknowledge}
            className="text-slate-400 transition hover:text-slate-700"
            aria-label="Dismiss warning"
            type="button"
          >
            <X size={20} />
          </button>
        </div>
        <div className="mt-6 flex flex-wrap items-center justify-end gap-3">
          <Button variant="secondary" onClick={acknowledge} type="button">
            Continue with default password
          </Button>
          <Button onClick={changeNow} type="button">
            Change password now
          </Button>
        </div>
      </div>
    </div>
  );
};
