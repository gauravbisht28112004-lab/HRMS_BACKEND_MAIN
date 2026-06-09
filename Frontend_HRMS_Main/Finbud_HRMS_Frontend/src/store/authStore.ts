import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { api, type PasswordChangePayload } from '@/services/api';
import { toAuthUser, type AuthUser } from '@/services/backendAdapters';
import { clearAuthTokens, getAccessToken, getRefreshToken, setAuthTokens } from '@/services/tokenStorage';

interface AuthState {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  login: (credentials: { username: string; password: string }) => Promise<void>;
  initializeSession: () => Promise<void>;
  logout: () => Promise<void>;
  clearSession: () => void;
  markSelfServiceEditUsed: () => void;
  /**
   * Patch the signed-in user's avatar URL. Called by AvatarUpload after a
   * self-service upload/remove so the Topbar and any other `<Avatar>` bound
   * to `user.avatarUrl` update without a full session refresh.
   */
  setAvatarUrl: (url: string | undefined) => void;
  /**
   * Rotate the currently signed-in user's password. On success we flip
   * `mustChangePassword` to false — the route guards can then release
   * the user from the force-change screen.
   */
  changePassword: (payload: PasswordChangePayload) => Promise<void>;
}

const enrichUser = async (user: AuthUser): Promise<AuthUser> => {
  if (!user.employeeId) {
    return user;
  }

  try {
    const employee = await api.employees.getRawByEmployeeId(user.employeeId);

    return {
      ...user,
      employeeDbId: employee.id,
      email: employee.email || user.email,
      name: employee.fullName || user.name,
      // Backend returns a presigned S3 URL when the user has uploaded an
      // avatar, or a legacy pasted URL from the admin form, or null if
      // they have neither. Any truthy value wins.
      avatarUrl: employee.profilePictureUrl ?? undefined,
    };
  } catch {
    return user;
  }
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isAuthenticated: false,
      isInitializing: true,
      login: async ({ username, password }) => {
        const response = await api.auth.login({ username, password });

        setAuthTokens({
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
        });

        // Always enrich with the employee profile. Password rotation is
        // now advisory (handled by the one-time PasswordChangeWarningModal)
        // rather than a hard block, so even must-change users get a full
        // session payload so the dashboard, topbar avatar, etc. render
        // correctly on first load.
        const baseUser = toAuthUser(response);
        const user = await enrichUser(baseUser);

        set({
          user,
          isAuthenticated: true,
          isInitializing: false,
        });
      },
      initializeSession: async () => {
        const accessToken = getAccessToken();
        const refreshToken = getRefreshToken();
        const persistedUser = get().user;

        if (!accessToken && !refreshToken) {
          set({
            user: null,
            isAuthenticated: false,
            isInitializing: false,
          });
          return;
        }

        try {
          if (!persistedUser || !accessToken) {
            if (!refreshToken) {
              throw new Error('Missing refresh token');
            }

            const response = await api.auth.refresh(refreshToken);

            setAuthTokens({
              accessToken: response.accessToken,
              refreshToken: response.refreshToken,
            });

            const baseUser = toAuthUser(response);
            const user = await enrichUser(baseUser);

            set({
              user,
              isAuthenticated: true,
              isInitializing: false,
            });
            return;
          }

          const user =
            persistedUser.employeeId && !persistedUser.employeeDbId
              ? await enrichUser(persistedUser)
              : persistedUser;

          set({
            user,
            isAuthenticated: true,
            isInitializing: false,
          });
        } catch {
          clearAuthTokens();
          set({
            user: null,
            isAuthenticated: false,
            isInitializing: false,
          });
        }
      },
      logout: async () => {
        try {
          if (getAccessToken()) {
            await api.auth.logout();
          }
        } catch {
          // Always clear the local session even if backend logout is unavailable.
        } finally {
          clearAuthTokens();
          set({
            user: null,
            isAuthenticated: false,
            isInitializing: false,
          });
        }
      },
      clearSession: () => {
        clearAuthTokens();
        set({
          user: null,
          isAuthenticated: false,
          isInitializing: false,
        });
      },
      markSelfServiceEditUsed: () =>
        set((state) => ({
          user: state.user
            ? {
                ...state.user,
                hasUsedSelfServiceEdit: true,
              }
            : null,
        })),
      setAvatarUrl: (url) =>
        set((state) => ({
          user: state.user
            ? {
                ...state.user,
                avatarUrl: url,
              }
            : null,
        })),
      changePassword: async (payload) => {
        await api.auth.changePassword(payload);

        const current = get().user;
        if (!current) return;

        // Backend clears the must-change flag on success; mirror that
        // locally and enrich the employee record now that the user has
        // graduated to a "real" session.
        const rotated: AuthUser = {
          ...current,
          mustChangePassword: false,
        };

        const user = rotated.employeeId && !rotated.employeeDbId ? await enrichUser(rotated) : rotated;

        set({ user });
      },
    }),
    {
      name: 'finbud-auth',
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);
