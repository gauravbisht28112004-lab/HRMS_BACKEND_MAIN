import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiError, BackendApiResponse, unwrapResponse } from '@/services/contracts';
import {
  clearAuthTokens,
  emitAuthExpired,
  getAccessToken,
  getRefreshToken,
  setAuthTokens,
  type AuthTokens,
} from '@/services/tokenStorage';

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
}

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

/**
 * `rawHttp` is used for refresh-token round-trips so the refresh call
 * never itself triggers the 401-retry interceptor.
 * `http` is the authenticated axios client used for every other call.
 *
 * Content-Type is NOT set as a default header any more — axios picks the
 * right value (`application/json` for plain objects,
 * `multipart/form-data; boundary=...` for FormData) only when no default
 * is present. Setting a hard `application/json` default broke file
 * uploads (see `api.admin.importEmployees`).
 */
export const rawHttp = axios.create({
  baseURL,
  timeout: 15000,
});

export const http = axios.create({
  baseURL,
  timeout: 15000,
});

const isAuthRoute = (url?: string) =>
  Boolean(
    url?.endsWith('/auth/login') ||
      url?.endsWith('/auth/refresh-token') ||
      url?.endsWith('/auth/register'),
  );

let refreshRequest: Promise<AuthTokens | null> | null = null;

const refreshAccessToken = async (): Promise<AuthTokens | null> => {
  const refreshToken = getRefreshToken();

  if (!refreshToken) {
    return null;
  }

  try {
    const response = await rawHttp.post<BackendApiResponse<RefreshTokenResponse>>('/auth/refresh-token', null, {
      params: { refreshToken },
    });
    const data = unwrapResponse(response);
    const tokens = {
      accessToken: data.accessToken,
      refreshToken: data.refreshToken ?? refreshToken,
    };

    setAuthTokens(tokens);
    return tokens;
  } catch {
    clearAuthTokens();
    emitAuthExpired();
    return null;
  }
};

http.interceptors.request.use((config) => {
  const token = getAccessToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // When the caller attaches a FormData body, axios sets the correct
  // multipart Content-Type with a boundary automatically *only if* we
  // don't override it. If a previous axios instance default or caller
  // has forced application/json, strip it here.
  if (config.data instanceof FormData && config.headers) {
    delete config.headers['Content-Type'];
    delete (config.headers as Record<string, unknown>)['content-type'];
  }

  return config;
});

/**
 * Convert an axios error-response body into an {@link ApiError} so callers
 * see the backend's `ApiResponse.message` / `errors[]` instead of the
 * opaque "Request failed with status code 500" default.
 *
 * Leaves network errors (no response) and 401s handled below untouched.
 */
const toApiError = (error: AxiosError): ApiError => {
  const status = error.response?.status;
  const body = error.response?.data as Partial<BackendApiResponse<unknown>> | undefined;

  // Prefer the backend's own error summary (set by GlobalExceptionHandler).
  // Fall back to the first entry in errors[], then axios's message.
  const message =
    (typeof body?.message === 'string' && body.message) ||
    (Array.isArray(body?.errors) && body!.errors!.length > 0 ? body!.errors![0] : undefined) ||
    error.message ||
    'Request failed.';

  return new ApiError(message, status, Array.isArray(body?.errors) ? body!.errors : undefined);
};

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;

    if (
      error.response?.status !== 401 ||
      !originalRequest ||
      originalRequest._retry ||
      isAuthRoute(originalRequest.url)
    ) {
      // Non-401 errors (500, 400, 403, etc.) — surface the server body.
      if (error.response) {
        return Promise.reject(toApiError(error));
      }
      return Promise.reject(error);
    }

    originalRequest._retry = true;
    refreshRequest ??= refreshAccessToken().finally(() => {
      refreshRequest = null;
    });

    const tokens = await refreshRequest;

    if (!tokens) {
      return Promise.reject(error);
    }

    originalRequest.headers.Authorization = `Bearer ${tokens.accessToken}`;
    return http(originalRequest);
  },
);

/**
 * Public helper so other modules (e.g. authStore) can trigger a proactive
 * refresh before a request 401s — useful when the UI knows the access
 * token is about to expire.
 */
export const refreshAccessTokenNow = (): Promise<AuthTokens | null> => {
  refreshRequest ??= refreshAccessToken().finally(() => {
    refreshRequest = null;
  });
  return refreshRequest;
};
