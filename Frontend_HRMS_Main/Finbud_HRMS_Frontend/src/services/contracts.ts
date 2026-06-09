import type { AxiosResponse } from 'axios';

export interface BackendApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errors?: string[];
  timestamp?: string;
  statusCode?: number;
}

export interface BackendPagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}

export interface UnwrappedPagedResponse<T> {
  items: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}

export class ApiError extends Error {
  statusCode?: number;
  errors?: string[];

  constructor(message: string, statusCode?: number, errors?: string[]) {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
    this.errors = errors;
  }
}

export const unwrapResponse = <T>(response: AxiosResponse<BackendApiResponse<T>>): T => {
  const payload = response.data;

  if (!payload.success) {
    throw new ApiError(
      payload.message ?? 'Request failed.',
      payload.statusCode ?? response.status,
      payload.errors,
    );
  }

  return payload.data;
};

export const unwrapPagedResponse = <T>(
  response: AxiosResponse<BackendApiResponse<BackendPagedResponse<T>>>,
): UnwrappedPagedResponse<T> => {
  const payload = unwrapResponse(response);

  return {
    items: payload.content ?? [],
    pageNumber: payload.pageNumber,
    pageSize: payload.pageSize,
    totalElements: payload.totalElements,
    totalPages: payload.totalPages,
    last: payload.last,
    first: payload.first,
    empty: payload.empty,
  };
};
