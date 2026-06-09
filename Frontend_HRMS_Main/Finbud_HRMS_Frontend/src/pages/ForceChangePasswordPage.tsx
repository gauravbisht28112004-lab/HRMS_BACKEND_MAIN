import { FormEvent, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { useAuthStore } from '@/store/authStore';

/**
 * Users provisioned via `api.admin.createUser` land here after login —
 * the backend flags them with `mustChangePassword=true` and the
 * `ProtectedRoute` gate steers every authenticated click to this page
 * until the credential is rotated.
 */
export const ForceChangePasswordPage = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated, changePassword } = useAuthStore();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  // Users who have already rotated shouldn't be able to linger here.
  if (!user.mustChangePassword) {
    return <Navigate to="/" replace />;
  }

  const validate = (): string | null => {
    if (!currentPassword) return 'Enter your temporary password.';
    if (newPassword.length < 8) return 'New password must be at least 8 characters.';
    if (newPassword === currentPassword) return 'New password must be different from the temporary password.';
    if (newPassword !== confirmPassword) return 'New password and confirmation do not match.';
    return null;
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setError('');
    setSubmitting(true);

    try {
      await changePassword({ currentPassword, newPassword, confirmPassword });
      navigate('/', { replace: true });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Unable to update password. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-8">
      <Card className="w-full max-w-md p-8">
        <div className="flex items-start gap-3">
          <span className="mt-1 inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-amber-50 text-amber-600">
            <ShieldAlert size={22} />
          </span>
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">Set a new password</h1>
            <p className="mt-1 text-sm text-slate-500">
              Your account was provisioned with a temporary password. Please choose a permanent one to continue.
            </p>
          </div>
        </div>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit} autoComplete="off">
          <Input
            label="Current (temporary) password"
            type="password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            autoComplete="current-password"
          />
          <Input
            label="New password"
            type="password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            autoComplete="new-password"
          />
          <Input
            label="Confirm new password"
            type="password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            autoComplete="new-password"
          />

          {error ? <p className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</p> : null}

          <Button type="submit" className="w-full" disabled={submitting}>
            {submitting ? 'Updating password...' : 'Update password'}
          </Button>
          <p className="text-center text-xs text-slate-400">
            Minimum 8 characters. Use something only you will remember.
          </p>
        </form>
      </Card>
    </div>
  );
};
