const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
const TRACE_ID_HEADER = 'X-Trace-Id';

const API_ROUTES = {
  authLogin: '/api/v1/auth/login',
  authRegister: '/api/v1/auth/register',
  authCurrentUser: '/api/v1/auth/me',
  health: '/api/v1/health',
  dictionaries: '/api/v1/dictionaries',
  dictionaryItems: (dictionaryCode: string) => `/api/v1/dictionaries/${dictionaryCode}/items`
} as const;

// Temporary compatibility layer for legacy endpoints.
// Deprecation date: 2026-09-30 (UTC).
const LEGACY_ROUTE_ALIASES: Record<string, string> = {
  '/api/v1/zidian': API_ROUTES.dictionaries,
  '/api/v1/zidian/:code/entries': '/api/v1/dictionaries/:code/items'
};

function resolveApiRoute(route: string): string {
  return LEGACY_ROUTE_ALIASES[route] ?? route;
}

function generateTraceId() {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
}

export type LoginPayload = { username: string; password: string };
export type RegisterPayload = { username: string; password: string };
export type AuthResponse = { accessToken: string; tokenType: string; expiresInSeconds: number };
export type CurrentUserResponse = { username: string; enabled: boolean; roles: string[]; permissions: string[] };
export type DictionaryResponse = { code: string; name: string };
export type DictionaryItemResponse = { id: number; name: string; value: string; sortOrder: number; enabled: boolean };

type ApiResponse<T> = { success: boolean; message?: string; data: T };
type ApiErrorResponse = { message?: string; errorCode?: string; details?: string[]; traceId?: string };

const errorCodeMessages: Record<string, string> = {
  VALIDATION_ERROR: 'Invalid request payload',
  CONFLICT: 'Request conflict. Please verify current data state',
  INTERNAL_ERROR: 'Unexpected server error. Please try again later',
  UNAUTHORIZED: 'Invalid username or password',
  FORBIDDEN: 'Insufficient permission for this action',
  NOT_FOUND: 'Requested resource was not found'
};

async function parseError(response: Response): Promise<Error> {
  const text = await response.text();
  try {
    const parsed = JSON.parse(text) as Partial<ApiErrorResponse>;
    if (parsed) {
      const human = parsed.errorCode ? errorCodeMessages[parsed.errorCode] ?? parsed.message ?? 'Request failed' : parsed.message ?? 'Request failed';
      const details = Array.isArray(parsed.details) && parsed.details.length > 0 ? `: ${parsed.details.join(', ')}` : '';
      const traceId = parsed.traceId ? ` (Trace ID: ${parsed.traceId})` : '';
      return new Error(`${human}${details}${traceId}`);
    }
  } catch {}
  return new Error(text || `Request failed with status ${response.status}`);
}

async function unwrapResponse<T>(response: Response): Promise<T> {
  if (!response.ok) throw await parseError(response);
  return (await response.json() as ApiResponse<T>).data;
}

async function apiFetch(route: string, init: RequestInit = {}): Promise<Response> {
  const traceId = generateTraceId();
  const headers = new Headers(init.headers);
  headers.set(TRACE_ID_HEADER, traceId);
  return fetch(`${API_BASE_URL}${resolveApiRoute(route)}`, { ...init, headers });
}

export async function login(payload: LoginPayload): Promise<AuthResponse> { return unwrapResponse(await apiFetch(API_ROUTES.authLogin,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function register(payload: RegisterPayload): Promise<void> { await unwrapResponse<Record<string,unknown>>(await apiFetch(API_ROUTES.authRegister,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function healthCheck(): Promise<{ status: string; service: string; timestamp: string }> { return unwrapResponse(await apiFetch(API_ROUTES.health,{cache:'no-store'})); }
export async function currentUser(token: string): Promise<CurrentUserResponse> { return unwrapResponse(await apiFetch(API_ROUTES.authCurrentUser,{headers:{Authorization:`Bearer ${token}`},cache:'no-store'})); }
export async function listDictionaries(token: string): Promise<DictionaryResponse[]> { const r=await apiFetch(API_ROUTES.dictionaries,{headers:{Authorization:`Bearer ${token}`},cache:'no-store'}); if(!r.ok) throw await parseError(r); return r.json(); }
export async function listDictionaryItems(token: string, dictionaryCode: string): Promise<DictionaryItemResponse[]> { const r=await apiFetch(API_ROUTES.dictionaryItems(dictionaryCode),{headers:{Authorization:`Bearer ${token}`},cache:'no-store'}); if(!r.ok) throw await parseError(r); return r.json(); }
