export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export const ACCESS_TOKEN_KEY = 'finbud_access_token';
export const REFRESH_TOKEN_KEY = 'finbud_refresh_token';
export const AUTH_EXPIRED_EVENT = 'finbud-auth-expired';

export const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY);

export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);

export const setAuthTokens = ({ accessToken, refreshToken }: AuthTokens) => {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
};

export const clearAuthTokens = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
};

export const emitAuthExpired = () => {
  window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
};
