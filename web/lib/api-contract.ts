export const API_ROUTES = {
  authLogin: '/api/v1/auth/login',
  authRegister: '/api/v1/auth/register',
  authCurrentUser: '/api/v1/auth/me',
  health: '/api/v1/health',
  dictionaries: '/api/v1/dictionaries',
  dictionaryItems: (dictionaryCode: string) => `/api/v1/dictionaries/${dictionaryCode}/items`,
  medications: '/api/v1/medications',
  familyDoctors: '/api/v1/family-doctors',
  visits: '/api/v1/visits',
  medicalRecords: '/api/v1/medical-records',
  healthRecords: '/api/v1/health-records',
  doctors: '/api/v1/doctors',
  configs: '/api/v1/configs'
} as const;

export type LoginPayload = { username: string; password: string };
export type RegisterPayload = { username: string; password: string };
export type AuthResponse = { accessToken: string; tokenType: string; expiresInSeconds: number };
export type CurrentUserResponse = { username: string; enabled: boolean; roles: string[]; permissions: string[] };
export type DictionaryResponse = { code: string; name: string };
export type DictionaryItemResponse = { id: number; name: string; value: string; sortOrder: number; enabled: boolean };
export type HealthResponse = { status: string; service: string; timestamp: string };
export type EntityRecord = { id: number; name?: string; enabled?: boolean; status?: string; [key: string]: unknown };

export type ApiResponse<T> = { success: boolean; message?: string; data: T };
export type ApiErrorResponse = { message?: string; errorCode?: string; details?: string[]; traceId?: string };

export const errorCodeMessages: Record<string, string> = {
  VALIDATION_ERROR: 'Invalid request payload',
  CONFLICT: 'Request conflict. Please verify current data state',
  INTERNAL_ERROR: 'Unexpected server error. Please try again later',
  UNAUTHORIZED: 'Invalid username or password',
  FORBIDDEN: 'Insufficient permission for this action',
  NOT_FOUND: 'Requested resource was not found'
};
