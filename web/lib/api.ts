import { API_ROUTES, type ApiErrorResponse, type ApiResponse, type AuthResponse, type CurrentUserResponse, type DictionaryItemResponse, type DictionaryResponse, type EntityRecord, errorCodeMessages, type HealthResponse, type LoginPayload, type RegisterPayload, type StatusChangeRequest, type StatusManagedRoute } from './api-contract';
export type { EntityRecord, CurrentUserResponse, DictionaryItemResponse, DictionaryResponse } from './api-contract';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
const TRACE_ID_HEADER = 'X-Trace-Id';

function generateTraceId() { return typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`; }
export type PageResponse<T> = { records: T[]; total: number; page: number; size: number };
export type ListQuery = { page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc'; keyword?: string; itemName?: string; enabled?: boolean };

async function parseError(response: Response): Promise<Error> {
  const text = await response.text();
  try {
    const parsed = JSON.parse(text) as Partial<ApiErrorResponse>;
    const human = parsed.errorCode ? errorCodeMessages[parsed.errorCode] ?? parsed.message ?? 'Request failed' : parsed.message ?? 'Request failed';
    return new Error(human);
  } catch { return new Error(text || `Request failed with status ${response.status}`); }
}
async function unwrapResponse<T>(response: Response): Promise<T> { if (!response.ok) throw await parseError(response); return (await response.json() as ApiResponse<T>).data; }
async function apiFetch(route: string, init: RequestInit = {}): Promise<Response> { const headers = new Headers(init.headers); headers.set(TRACE_ID_HEADER, generateTraceId()); return fetch(`${API_BASE_URL}${route}`, { ...init, headers }); }
const authHeader = (token: string) => ({ Authorization: `Bearer ${token}` });
const statusChangeByRoute: { [R in StatusManagedRoute]: (enabled: boolean) => StatusChangeRequest<R> } = {
  [API_ROUTES.doctors]: (enabled) => ({ targetStatus: enabled ? 'ACTIVE' : 'INACTIVE' }),
  [API_ROUTES.familyDoctors]: (enabled) => ({ targetStatus: enabled ? 'ACTIVE' : 'SUSPENDED' }),
  [API_ROUTES.visits]: (enabled) => ({ targetStatus: enabled ? 'COMPLETED' : 'CANCELLED' }),
  [API_ROUTES.medications]: (enabled) => ({ targetStatus: enabled ? 'ENABLED' : 'DISABLED' }),
  [API_ROUTES.configs]: (enabled) => ({ targetStatus: enabled ? 'ENABLED' : 'DISABLED' }),
  [API_ROUTES.medicalRecords]: (enabled) => ({ targetStatus: enabled ? 'ACTIVE' : 'ARCHIVED' }),
  [API_ROUTES.healthRecords]: (enabled) => ({ targetStatus: enabled ? 'ACTIVE' : 'ARCHIVED' })
};

export async function login(payload: LoginPayload): Promise<AuthResponse> { return unwrapResponse(await apiFetch(API_ROUTES.authLogin,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function register(payload: RegisterPayload): Promise<void> { await unwrapResponse<Record<string,unknown>>(await apiFetch(API_ROUTES.authRegister,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function healthCheck(): Promise<HealthResponse> { return unwrapResponse(await apiFetch(API_ROUTES.health,{cache:'no-store'})); }
export async function currentUser(token: string): Promise<CurrentUserResponse> { return unwrapResponse(await apiFetch(API_ROUTES.authCurrentUser,{headers:authHeader(token),cache:'no-store'})); }
export async function listDictionaries(token: string): Promise<DictionaryResponse[]> { return unwrapResponse(await apiFetch(API_ROUTES.dictionaries,{headers:authHeader(token),cache:'no-store'})); }
export async function queryDictionaryItems(token: string, query: ListQuery & { dictCode: string }): Promise<PageResponse<DictionaryItemResponse>> {
  const p = new URLSearchParams(); Object.entries(query).forEach(([k,v])=>v!==undefined&&p.set(k,String(v)));
  return unwrapResponse(await apiFetch(`${API_ROUTES.dictionaryItems(query.dictCode)}?${p.toString()}`, { headers: authHeader(token), cache:'no-store' }));
}
export async function listEntities(token: string, route: string, query: ListQuery = {}): Promise<PageResponse<EntityRecord>> {
  const p = new URLSearchParams(); Object.entries(query).forEach(([k,v])=>v!==undefined&&p.set(k,String(v)));
  return unwrapResponse(await apiFetch(`${route}?${p.toString()}`, { headers: authHeader(token), cache:'no-store' }));
}
export async function getEntity(token: string, route: string, id: number): Promise<EntityRecord> { return unwrapResponse(await apiFetch(`${route}/${id}`, { headers: authHeader(token), cache:'no-store' })); }
export async function createEntity(token: string, route: string, payload: Record<string, unknown>): Promise<void> { await unwrapResponse(await apiFetch(route,{method:'POST',headers:{...authHeader(token),'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function updateEntity(token: string, route: string, id: number, payload: Record<string, unknown>): Promise<void> { await unwrapResponse(await apiFetch(`${route}/${id}`,{method:'PUT',headers:{...authHeader(token),'Content-Type':'application/json'},body:JSON.stringify(payload)})); }
export async function changeEntityStatus(token: string, route: string, id: number, enabled: boolean): Promise<void> {
  const payload = route in statusChangeByRoute
    ? statusChangeByRoute[route as StatusManagedRoute](enabled)
    : ({ enabled } as Record<string, unknown>);
  await unwrapResponse(await apiFetch(`${route}/${id}/status`,{method:'PATCH',headers:{...authHeader(token),'Content-Type':'application/json'},body:JSON.stringify(payload)}));
}
